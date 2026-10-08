package com.communityfreshmarket.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.communityfreshmarket.address.AddressService;
import com.communityfreshmarket.auth.UserContext;
import com.communityfreshmarket.cart.CartService;
import com.communityfreshmarket.cart.dto.CartLineResponse;
import com.communityfreshmarket.cart.dto.CartResponse;
import com.communityfreshmarket.catalog.ProductQtyRules;
import com.communityfreshmarket.catalog.StockInventoryService;
import com.communityfreshmarket.coupon.CouponService;
import com.communityfreshmarket.common.BizException;
import com.communityfreshmarket.common.ErrorCodes;
import com.communityfreshmarket.domain.Order;
import com.communityfreshmarket.domain.OrderItem;
import com.communityfreshmarket.domain.PickupSlot;
import com.communityfreshmarket.domain.Product;
import com.communityfreshmarket.domain.Store;
import com.communityfreshmarket.domain.UserAddress;
import com.communityfreshmarket.mapper.OrderItemMapper;
import com.communityfreshmarket.mapper.OrderMapper;
import com.communityfreshmarket.mapper.ProductMapper;
import com.communityfreshmarket.mapper.StoreMapper;
import com.communityfreshmarket.notify.OrderNotifyService;
import com.communityfreshmarket.points.PointsService;
import com.communityfreshmarket.order.dto.CancelOrderRequest;
import com.communityfreshmarket.order.dto.MerchantOrderBoardResponse;
import com.communityfreshmarket.order.dto.OrderItemResponse;
import com.communityfreshmarket.order.dto.OrderResponse;
import com.communityfreshmarket.order.dto.ReorderResponse;
import com.communityfreshmarket.order.dto.SubmitOrderRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OrderService {

  private static final Logger log = LoggerFactory.getLogger(OrderService.class);

  private final OrderMapper orderMapper;
  private final OrderItemMapper orderItemMapper;
  private final CartService cartService;
  private final AddressService addressService;
  private final PickupSlotService pickupSlotService;
  private final ProductMapper productMapper;
  private final ObjectMapper objectMapper;
  private final OrderNotifyService orderNotifyService;
  private final StockInventoryService stockInventoryService;
  private final PointsService pointsService;
  private final CouponService couponService;
  private final StoreMapper storeMapper;

  public OrderService(
      OrderMapper orderMapper,
      OrderItemMapper orderItemMapper,
      CartService cartService,
      AddressService addressService,
      PickupSlotService pickupSlotService,
      ProductMapper productMapper,
      ObjectMapper objectMapper,
      OrderNotifyService orderNotifyService,
      StockInventoryService stockInventoryService,
      PointsService pointsService,
      CouponService couponService,
      StoreMapper storeMapper) {
    this.orderMapper = orderMapper;
    this.orderItemMapper = orderItemMapper;
    this.cartService = cartService;
    this.addressService = addressService;
    this.pickupSlotService = pickupSlotService;
    this.productMapper = productMapper;
    this.objectMapper = objectMapper;
    this.orderNotifyService = orderNotifyService;
    this.stockInventoryService = stockInventoryService;
    this.pointsService = pointsService;
    this.couponService = couponService;
    this.storeMapper = storeMapper;
  }

  @Transactional
  public OrderResponse submit(SubmitOrderRequest request) {
    long userId = UserContext.requireUserId();
    long storeId = UserContext.storeId();
    FulfillmentType fulfillment = FulfillmentType.require(request.getFulfillment());

    CartResponse cart = cartService.getCart();
    List<CartLineResponse> available = cart.getItems().stream()
        .filter(CartLineResponse::isAvailable)
        .toList();
    if (available.isEmpty()) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "商品篮没有可预约的商品");
    }

    // 二次校验：下单瞬间仍须上架有货，且数量符合起购/限购/库存
    for (CartLineResponse line : available) {
      Product product = productMapper.selectById(line.getProductId());
      if (product == null || !Objects.equals(product.getStoreId(), storeId) || product.getDeletedAt() != null) {
        throw new BizException(ErrorCodes.BAD_REQUEST, line.getName() + " 已不可购买，请调整商品篮");
      }
      if (!Boolean.TRUE.equals(product.getOnShelf())) {
        throw new BizException(ErrorCodes.BAD_REQUEST, line.getName() + " 已下架，请调整商品篮");
      }
      if (!Boolean.TRUE.equals(product.getInStock())) {
        throw new BizException(ErrorCodes.BAD_REQUEST, line.getName() + " 已售罄，请调整商品篮");
      }
      if (product.getStockQty() != null && product.getStockQty().compareTo(line.getQty()) < 0) {
        throw new BizException(ErrorCodes.BAD_REQUEST, line.getName() + " 库存不足，请调整商品篮");
      }
      try {
        ProductQtyRules.validatePurchasableQty(product, line.getQty());
      } catch (BizException ex) {
        throw new BizException(ErrorCodes.BAD_REQUEST, line.getName() + "：" + ex.getMessage());
      }
    }

    LocalDateTime now = LocalDateTime.now();
    Order order = new Order();
    order.setStoreId(storeId);
    order.setUserId(userId);
    order.setFulfillment(fulfillment.name());
    order.setStatus(OrderStatus.PENDING_CONFIRM.name());
    order.setAsap(fulfillment == FulfillmentType.DELIVERY
        && (request.getAsap() == null || request.getAsap()));
    order.setRemark(StringUtils.hasText(request.getRemark()) ? request.getRemark().trim() : null);
    order.setMerchantUnread(true);
    order.setCreatedAt(now);
    order.setUpdatedAt(now);

    if (fulfillment == FulfillmentType.DELIVERY) {
      if (request.getAddressId() == null) {
        throw new BizException(ErrorCodes.BAD_REQUEST, "请选择收货地址");
      }
      UserAddress address = addressService.requireOwned(request.getAddressId());
      order.setAddressSnapshot(toAddressJson(address));
      order.setPickupSlotId(null);
      order.setPickupSlotLabel(null);
    } else {
      if (request.getPickupSlotId() == null) {
        throw new BizException(ErrorCodes.BAD_REQUEST, "请选择自提时段");
      }
      PickupSlot slot = pickupSlotService.requireEnabled(request.getPickupSlotId(), storeId);
      order.setPickupSlotId(slot.getId());
      order.setPickupSlotLabel(slot.getLabel());
      order.setAddressSnapshot(null);
      order.setAsap(false);
    }

    int total = available.stream().mapToInt(CartLineResponse::getAmountFen).sum();
    if (fulfillment == FulfillmentType.DELIVERY) {
      Store store = storeMapper.selectById(storeId);
      int minDeliveryFen = store == null || store.getMinDeliveryFen() == null ? 0 : store.getMinDeliveryFen();
      if (minDeliveryFen > 0 && total < minDeliveryFen) {
        throw new BizException(ErrorCodes.BAD_REQUEST, "未满配送起送金额");
      }
    }
    order.setTotalAmountFen(total);
    order.setDiscountFen(0);
    orderMapper.insert(order);
    if (request.getCouponClaimId() != null) {
      int discount = couponService.reserveForOrder(
          request.getCouponClaimId(), userId, storeId, order.getId(), total);
      order.setCouponClaimId(request.getCouponClaimId());
      order.setDiscountFen(discount);
      orderMapper.updateById(order);
    }

    List<Long> purchasedIds = new ArrayList<>();
    for (CartLineResponse line : available) {
      OrderItem item = new OrderItem();
      item.setOrderId(order.getId());
      item.setStoreId(storeId);
      item.setProductId(line.getProductId());
      item.setProductName(line.getName());
      item.setUnit(line.getUnit());
      item.setUnitPriceFen(line.getPriceFen());
      item.setQty(line.getQty());
      item.setAmountFen(line.getAmountFen());
      item.setCreatedAt(now);
      orderItemMapper.insert(item);
      stockInventoryService.deduct(line.getProductId(), storeId, line.getQty());
      purchasedIds.add(line.getProductId());
    }

    cartService.removeProducts(purchasedIds);
    log.info("order created id={} storeId={} userId={} totalFen={}",
        order.getId(), storeId, userId, total);
    orderNotifyService.notifyNewOrder(order);
    return toResponse(order);
  }

  public List<OrderResponse> listMine(String status) {
    long userId = UserContext.requireUserId();
    long storeId = UserContext.storeId();
    LambdaQueryWrapper<Order> q = new LambdaQueryWrapper<Order>()
        .eq(Order::getUserId, userId)
        .eq(Order::getStoreId, storeId)
        .orderByDesc(Order::getCreatedAt);
    if (StringUtils.hasText(status)) {
      q.eq(Order::getStatus, OrderStatus.require(status).name());
    }
    return orderMapper.selectList(q).stream().map(this::toResponse).toList();
  }

  public OrderResponse detailMine(long id) {
    return toResponse(requireMine(id));
  }

  @Transactional
  public OrderResponse cancelMine(long id, CancelOrderRequest request) {
    Order order = requireMine(id);
    cancel(order, "CUSTOMER", request == null ? null : request.getReason());
    return toResponse(orderMapper.selectById(id));
  }

  @Transactional
  public OrderResponse arrive(long id) {
    Order order = requireMine(id);
    OrderStatus st = OrderStatus.require(order.getStatus());
    if (st != OrderStatus.READY_PICKUP) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "当前状态不可报到");
    }
    if (order.getCustomerArrivedAt() != null) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "已报到，无需重复操作");
    }
    LocalDateTime now = LocalDateTime.now();
    order.setCustomerArrivedAt(now);
    order.setMerchantUnread(true);
    order.setUpdatedAt(now);
    orderMapper.updateById(order);
    log.info("customer arrived orderId={}", id);
    orderNotifyService.notifyCustomerArrived(order);
    return toResponse(order);
  }

  @Transactional
  public ReorderResponse reorder(long id) {
    Order order = requireMine(id);
    if (OrderStatus.require(order.getStatus()) != OrderStatus.COMPLETED) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "仅已完成订单可再来一单");
    }
    List<OrderItem> items = listItems(id);
    List<String> skipped = new ArrayList<>();
    for (OrderItem item : items) {
      if (item.getProductId() == null) {
        skipped.add(item.getProductName() + "（已失效）");
        continue;
      }
      String reason = cartService.tryAdd(item.getProductId(), item.getQty());
      if (reason != null) {
        skipped.add(item.getProductName() + "（" + reason + "）");
      }
    }
    return new ReorderResponse(cartService.getCart(), skipped);
  }

  public MerchantOrderBoardResponse merchantBoard() {
    long storeId = UserContext.storeId();
    List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
        .eq(Order::getStoreId, storeId)
        .in(Order::getStatus, OrderStatus.UNFINISHED.stream().map(Enum::name).toList())
        .orderByDesc(Order::getMerchantUnread)
        .orderByAsc(Order::getCreatedAt));

    List<OrderResponse> urgent = new ArrayList<>();
    List<OrderResponse> others = new ArrayList<>();
    for (Order order : orders) {
      OrderResponse r = toResponse(order);
      if (isUrgent(order)) {
        urgent.add(r);
      } else {
        others.add(r);
      }
    }
    return new MerchantOrderBoardResponse(urgent, others);
  }

  public OrderResponse merchantDetail(long id) {
    Order order = requireStoreOrder(id);
    if (Boolean.TRUE.equals(order.getMerchantUnread())) {
      order.setMerchantUnread(false);
      order.setUpdatedAt(LocalDateTime.now());
      orderMapper.updateById(order);
    }
    return toResponse(order);
  }

  @Transactional
  public OrderResponse advance(long id, String expectedStatus) {
    Order order = requireStoreOrder(id);
    OrderStatus current = OrderStatus.require(order.getStatus());
    OrderStatus expected = OrderStatus.require(expectedStatus);
    if (current != expected) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "订单状态已变化，请刷新后重试");
    }
    OrderStatus next = OrderStatus.next(current, order.getFulfillment());
    int updated = orderMapper.update(
        null,
        new LambdaUpdateWrapper<Order>()
            .eq(Order::getId, id)
            .eq(Order::getStatus, current.name())
            .set(Order::getStatus, next.name())
            .set(Order::getMerchantUnread, false)
            .set(Order::getUpdatedAt, LocalDateTime.now()));
    if (updated == 0) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "订单状态已变化，请刷新后重试");
    }
    Order fresh = orderMapper.selectById(id);
    if (OrderStatus.require(fresh.getStatus()) == OrderStatus.COMPLETED) {
      couponService.useByOrder(fresh.getId());
      pointsService.awardOrderComplete(fresh);
    }
    return toResponse(fresh);
  }

  @Transactional
  public OrderResponse cancelMerchant(long id, CancelOrderRequest request) {
    Order order = requireStoreOrder(id);
    cancel(order, "MERCHANT", request == null ? null : request.getReason());
    return toResponse(orderMapper.selectById(id));
  }

  private void cancel(Order order, String by, String reason) {
    OrderStatus st = OrderStatus.require(order.getStatus());
    if (!st.unfinished()) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "订单已结束，不可取消");
    }
    LocalDateTime now = LocalDateTime.now();
    int updated = orderMapper.update(
        null,
        new LambdaUpdateWrapper<Order>()
            .eq(Order::getId, order.getId())
            .eq(Order::getStatus, st.name())
            .set(Order::getStatus, OrderStatus.CANCELLED.name())
            .set(Order::getCancelBy, by)
            .set(Order::getCancelReason, StringUtils.hasText(reason) ? reason.trim() : null)
            .set(Order::getCancelledAt, now)
            .set(Order::getMerchantUnread, "CUSTOMER".equals(by))
            .set(Order::getUpdatedAt, now));
    if (updated == 0) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "订单状态已变化，请刷新后重试");
    }
    restoreStock(order.getId(), order.getStoreId());
    couponService.releaseByOrder(order.getId());
    log.info("order cancelled id={} by={}", order.getId(), by);
    Order fresh = orderMapper.selectById(order.getId());
    if (fresh != null) {
      orderNotifyService.notifyCancel(fresh, by);
    }
  }

  private void restoreStock(long orderId, long storeId) {
    for (OrderItem item : listItems(orderId)) {
      if (item.getProductId() != null && item.getQty() != null) {
        stockInventoryService.restore(item.getProductId(), storeId, item.getQty());
      }
    }
  }

  private boolean isUrgent(Order order) {
    OrderStatus st = OrderStatus.require(order.getStatus());
    if (st == OrderStatus.PENDING_CONFIRM) {
      return true;
    }
    return order.getCustomerArrivedAt() != null && st.unfinished();
  }

  private Order requireMine(long id) {
    long userId = UserContext.requireUserId();
    long storeId = UserContext.storeId();
    Order order = orderMapper.selectById(id);
    if (order == null
        || !order.getUserId().equals(userId)
        || !order.getStoreId().equals(storeId)) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "订单不存在");
    }
    return order;
  }

  private Order requireStoreOrder(long id) {
    long storeId = UserContext.storeId();
    Order order = orderMapper.selectById(id);
    if (order == null || !order.getStoreId().equals(storeId)) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "订单不存在");
    }
    return order;
  }

  private OrderResponse toResponse(Order order) {
    List<OrderItemResponse> items = listItems(order.getId()).stream()
        .map(OrderItemResponse::from)
        .toList();
    return OrderResponse.from(order, items);
  }

  private List<OrderItem> listItems(long orderId) {
    return orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
        .eq(OrderItem::getOrderId, orderId)
        .orderByAsc(OrderItem::getId));
  }

  private String toAddressJson(UserAddress address) {
    try {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("id", address.getId());
      map.put("phone", address.getPhone());
      map.put("community", address.getCommunity());
      map.put("building", address.getBuilding());
      map.put("room", address.getRoom());
      return objectMapper.writeValueAsString(map);
    } catch (Exception ex) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "地址快照失败");
    }
  }
}
