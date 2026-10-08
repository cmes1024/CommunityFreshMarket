package com.communityfreshmarket.cashier;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.communityfreshmarket.auth.UserContext;
import com.communityfreshmarket.cart.CartService;
import com.communityfreshmarket.catalog.StockInventoryService;
import com.communityfreshmarket.common.BizException;
import com.communityfreshmarket.common.ErrorCodes;
import com.communityfreshmarket.coupon.CouponService;
import com.communityfreshmarket.domain.CashierSale;
import com.communityfreshmarket.domain.CashierSaleItem;
import com.communityfreshmarket.domain.Product;
import com.communityfreshmarket.mapper.CashierSaleItemMapper;
import com.communityfreshmarket.mapper.CashierSaleMapper;
import com.communityfreshmarket.mapper.ProductMapper;
import com.communityfreshmarket.points.PointsService;
import com.communityfreshmarket.cashier.dto.CashierAddItemRequest;
import com.communityfreshmarket.cashier.dto.CashierCompleteRequest;
import com.communityfreshmarket.cashier.dto.CashierCouponRequest;
import com.communityfreshmarket.cashier.dto.CashierSaleResponse;
import com.communityfreshmarket.checkin.CheckinService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CashierService {

  private final CashierSaleMapper saleMapper;
  private final CashierSaleItemMapper itemMapper;
  private final ProductMapper productMapper;
  private final PointsService pointsService;
  private final CheckinService checkinService;
  private final CouponService couponService;
  private final StockInventoryService stockInventoryService;

  public CashierService(
      CashierSaleMapper saleMapper,
      CashierSaleItemMapper itemMapper,
      ProductMapper productMapper,
      PointsService pointsService,
      CheckinService checkinService,
      CouponService couponService,
      StockInventoryService stockInventoryService) {
    this.saleMapper = saleMapper;
    this.itemMapper = itemMapper;
    this.productMapper = productMapper;
    this.pointsService = pointsService;
    this.checkinService = checkinService;
    this.couponService = couponService;
    this.stockInventoryService = stockInventoryService;
  }

  @Transactional
  public CashierSaleResponse open() {
    long storeId = UserContext.storeId();
    long userId = UserContext.requireUserId();
    LocalDateTime now = LocalDateTime.now();
    CashierSale sale = new CashierSale();
    sale.setStoreId(storeId);
    sale.setStatus("OPEN");
    sale.setTotalFen(0);
    sale.setDiscountFen(0);
    sale.setCouponDiscountFen(0);
    sale.setPayFen(0);
    sale.setCreatedBy(userId);
    sale.setCreatedAt(now);
    sale.setUpdatedAt(now);
    saleMapper.insert(sale);
    return toResponse(sale);
  }

  public CashierSaleResponse detail(long id) {
    return toResponse(requireOwned(id));
  }

  public List<CashierSaleResponse> listOpen() {
    long storeId = UserContext.storeId();
    return saleMapper.selectList(new LambdaQueryWrapper<CashierSale>()
            .eq(CashierSale::getStoreId, storeId)
            .eq(CashierSale::getStatus, "OPEN")
            .orderByDesc(CashierSale::getId))
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public CashierSaleResponse addItem(long saleId, CashierAddItemRequest request) {
    CashierSale sale = requireOpen(saleId);
    if (request == null || request.getProductId() == null) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "请选择商品");
    }
    Product product = productMapper.selectById(request.getProductId());
    if (product == null || !product.getStoreId().equals(sale.getStoreId()) || product.getDeletedAt() != null) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "商品不存在");
    }
    BigDecimal qty = request.getQty() == null ? BigDecimal.ONE : request.getQty();
    if (qty.compareTo(BigDecimal.ZERO) <= 0) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "数量须大于 0");
    }
    int unitPrice = request.getUnitPriceFen() != null
        ? request.getUnitPriceFen()
        : (product.getPriceFen() == null ? 0 : product.getPriceFen());
    assertSellable(product, sale.getId(), qty);
    int amount = CartService.calcAmountFen(unitPrice, qty);
    boolean adjusted = request.getUnitPriceFen() != null
        && product.getPriceFen() != null
        && !request.getUnitPriceFen().equals(product.getPriceFen());

    CashierSaleItem existing = listItems(sale.getId()).stream()
        .filter(i -> product.getId().equals(i.getProductId())
            && i.getUnitPriceFen() != null
            && i.getUnitPriceFen() == unitPrice)
        .findFirst()
        .orElse(null);
    if (existing != null) {
      BigDecimal nextQty = existing.getQty().add(qty);
      existing.setQty(nextQty);
      existing.setAmountFen(CartService.calcAmountFen(unitPrice, nextQty));
      existing.setAdjusted(Boolean.TRUE.equals(existing.getAdjusted()) || adjusted);
      itemMapper.updateById(existing);
    } else {
      CashierSaleItem item = new CashierSaleItem();
      item.setSaleId(sale.getId());
      item.setStoreId(sale.getStoreId());
      item.setProductId(product.getId());
      item.setProductName(product.getName());
      item.setUnit(product.getUnit());
      item.setUnitPriceFen(unitPrice);
      item.setQty(qty);
      item.setAmountFen(amount);
      item.setAdjusted(adjusted);
      item.setCreatedAt(LocalDateTime.now());
      itemMapper.insert(item);
    }

    recalculate(sale);
    return toResponse(sale);
  }

  @Transactional
  public CashierSaleResponse removeItem(long saleId, long itemId) {
    CashierSale sale = requireOpen(saleId);
    CashierSaleItem item = itemMapper.selectById(itemId);
    if (item == null || !sale.getId().equals(item.getSaleId())) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "商品行不存在");
    }
    itemMapper.deleteById(itemId);
    recalculate(sale);
    return toResponse(sale);
  }

  @Transactional
  public CashierSaleResponse applyCoupon(long saleId, CashierCouponRequest request) {
    CashierSale sale = requireOpen(saleId);
    clearCoupon(sale);
    Long claimId = request == null ? null : request.getClaimId();
    if (claimId != null) {
      if (sale.getMemberUserId() == null) {
        throw new BizException(ErrorCodes.BAD_REQUEST, "请先挂会员再用券");
      }
      int total = listItems(sale.getId()).stream()
          .mapToInt(i -> i.getAmountFen() == null ? 0 : i.getAmountFen())
          .sum();
      int discount = couponService.reserveForCashier(
          claimId, sale.getMemberUserId(), sale.getStoreId(), sale.getId(), total);
      sale.setCouponClaimId(claimId);
      sale.setCouponDiscountFen(discount);
    }
    recalculate(sale);
    return toResponse(sale);
  }

  @Transactional
  public CashierSaleResponse attachMember(long saleId, Long memberUserId, Long checkinId) {
    CashierSale sale = requireOpen(saleId);
    Long uid = memberUserId;
    if (checkinId != null) {
      uid = checkinService.consume(checkinId, sale.getStoreId());
    }
    if (!Objects.equals(uid, sale.getMemberUserId())) {
      clearCoupon(sale);
    }
    sale.setMemberUserId(uid);
    sale.setUpdatedAt(LocalDateTime.now());
    saleMapper.updateById(sale);
    return toResponse(sale);
  }

  @Transactional
  public CashierSaleResponse complete(long saleId, CashierCompleteRequest request) {
    CashierSale sale = requireOpen(saleId);
    List<CashierSaleItem> items = listItems(sale.getId());
    if (items.isEmpty()) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "请先添加商品");
    }
    int manual = request != null && request.getDiscountFen() != null ? request.getDiscountFen() : 0;
    if (manual < 0) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "优惠不能为负");
    }
    int total = sale.getTotalFen() == null ? 0 : sale.getTotalFen();
    int coupon = sale.getCouponDiscountFen() == null ? 0 : sale.getCouponDiscountFen();
    int discount = Math.min(total, coupon + manual);
    int pay = total - discount;
    LocalDateTime now = LocalDateTime.now();
    sale.setDiscountFen(discount);
    sale.setPayFen(pay);
    sale.setAdjustReason(request != null && StringUtils.hasText(request.getAdjustReason())
        ? request.getAdjustReason().trim()
        : null);
    sale.setStatus("COMPLETED");
    sale.setCompletedAt(now);
    sale.setUpdatedAt(now);
    saleMapper.updateById(sale);
    for (CashierSaleItem item : items) {
      if (item.getProductId() != null && item.getQty() != null) {
        stockInventoryService.deduct(item.getProductId(), sale.getStoreId(), item.getQty());
      }
    }
    if (sale.getCouponClaimId() != null) {
      couponService.useByCashier(sale.getId());
    }
    if (sale.getMemberUserId() != null && pay > 0) {
      pointsService.awardCashierComplete(sale.getMemberUserId(), sale.getStoreId(), sale.getId(), pay);
    }
    return toResponse(sale);
  }

  @Transactional
  public CashierSaleResponse cancel(long saleId) {
    CashierSale sale = requireOpen(saleId);
    clearCoupon(sale);
    sale.setStatus("CANCELLED");
    sale.setUpdatedAt(LocalDateTime.now());
    saleMapper.updateById(sale);
    return toResponse(sale);
  }

  private void clearCoupon(CashierSale sale) {
    if (sale.getCouponClaimId() == null) {
      return;
    }
    couponService.releaseClaim(sale.getCouponClaimId());
    sale.setCouponClaimId(null);
    sale.setCouponDiscountFen(0);
    saleMapper.update(
        null,
        new LambdaUpdateWrapper<CashierSale>()
            .eq(CashierSale::getId, sale.getId())
            .set(CashierSale::getCouponClaimId, null)
            .set(CashierSale::getCouponDiscountFen, 0));
  }

  private void assertSellable(Product product, long saleId, BigDecimal addQty) {
    BigDecimal already = listItems(saleId).stream()
        .filter(i -> product.getId().equals(i.getProductId()))
        .map(i -> i.getQty() == null ? BigDecimal.ZERO : i.getQty())
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal need = already.add(addQty);
    if (product.getStockQty() == null) {
      if (!Boolean.TRUE.equals(product.getInStock())) {
        throw new BizException(ErrorCodes.BAD_REQUEST, product.getName() + " 已售罄");
      }
      return;
    }
    if (product.getStockQty().compareTo(need) < 0) {
      throw new BizException(ErrorCodes.BAD_REQUEST, product.getName() + " 库存不足");
    }
  }

  private void recalculate(CashierSale sale) {
    List<CashierSaleItem> items = listItems(sale.getId());
    int total = items.stream().mapToInt(i -> i.getAmountFen() == null ? 0 : i.getAmountFen()).sum();
    int couponDiscount = couponService.syncCashierDiscount(sale.getCouponClaimId(), total);
    if (couponDiscount < 0) {
      sale.setCouponClaimId(null);
      couponDiscount = 0;
      saleMapper.update(
          null,
          new LambdaUpdateWrapper<CashierSale>()
              .eq(CashierSale::getId, sale.getId())
              .set(CashierSale::getCouponClaimId, null));
    }
    sale.setTotalFen(total);
    sale.setCouponDiscountFen(couponDiscount);
    sale.setDiscountFen(couponDiscount);
    sale.setPayFen(Math.max(0, total - couponDiscount));
    sale.setUpdatedAt(LocalDateTime.now());
    saleMapper.updateById(sale);
  }

  private CashierSale requireOwned(long id) {
    long storeId = UserContext.storeId();
    CashierSale sale = saleMapper.selectById(id);
    if (sale == null || !sale.getStoreId().equals(storeId)) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "收银单不存在");
    }
    return sale;
  }

  private CashierSale requireOpen(long id) {
    CashierSale sale = requireOwned(id);
    if (!"OPEN".equals(sale.getStatus())) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "收银单已结束");
    }
    return sale;
  }

  private List<CashierSaleItem> listItems(long saleId) {
    return itemMapper.selectList(new LambdaQueryWrapper<CashierSaleItem>()
        .eq(CashierSaleItem::getSaleId, saleId)
        .orderByAsc(CashierSaleItem::getId));
  }

  private CashierSaleResponse toResponse(CashierSale sale) {
    return CashierSaleResponse.from(sale, listItems(sale.getId()));
  }
}
