package com.communityfreshmarket.cashier;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.communityfreshmarket.auth.UserContext;
import com.communityfreshmarket.cart.CartService;
import com.communityfreshmarket.common.BizException;
import com.communityfreshmarket.common.ErrorCodes;
import com.communityfreshmarket.domain.CashierSale;
import com.communityfreshmarket.domain.CashierSaleItem;
import com.communityfreshmarket.domain.Product;
import com.communityfreshmarket.mapper.CashierSaleItemMapper;
import com.communityfreshmarket.mapper.CashierSaleMapper;
import com.communityfreshmarket.mapper.ProductMapper;
import com.communityfreshmarket.points.PointsService;
import com.communityfreshmarket.cashier.dto.CashierAddItemRequest;
import com.communityfreshmarket.cashier.dto.CashierCompleteRequest;
import com.communityfreshmarket.cashier.dto.CashierSaleResponse;
import com.communityfreshmarket.checkin.CheckinService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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

  public CashierService(
      CashierSaleMapper saleMapper,
      CashierSaleItemMapper itemMapper,
      ProductMapper productMapper,
      PointsService pointsService,
      CheckinService checkinService) {
    this.saleMapper = saleMapper;
    this.itemMapper = itemMapper;
    this.productMapper = productMapper;
    this.pointsService = pointsService;
    this.checkinService = checkinService;
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
    if (product == null || !product.getStoreId().equals(sale.getStoreId())) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "商品不存在");
    }
    BigDecimal qty = request.getQty() == null ? BigDecimal.ONE : request.getQty();
    if (qty.compareTo(BigDecimal.ZERO) <= 0) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "数量须大于 0");
    }
    int unitPrice = request.getUnitPriceFen() != null
        ? request.getUnitPriceFen()
        : (product.getPriceFen() == null ? 0 : product.getPriceFen());
    int amount = CartService.calcAmountFen(unitPrice, qty);
    boolean adjusted = request.getUnitPriceFen() != null
        && product.getPriceFen() != null
        && !request.getUnitPriceFen().equals(product.getPriceFen());

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
    sale.setMemberUserId(uid);
    sale.setUpdatedAt(LocalDateTime.now());
    saleMapper.updateById(sale);
    return toResponse(sale);
  }

  @Transactional
  public CashierSaleResponse complete(long saleId, CashierCompleteRequest request) {
    CashierSale sale = requireOpen(saleId);
    int discount = request != null && request.getDiscountFen() != null ? request.getDiscountFen() : 0;
    if (discount < 0) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "优惠不能为负");
    }
    int total = sale.getTotalFen() == null ? 0 : sale.getTotalFen();
    if (discount > total) {
      discount = total;
    }
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

    if (sale.getMemberUserId() != null && pay > 0) {
      pointsService.awardCashierComplete(sale.getMemberUserId(), sale.getStoreId(), sale.getId(), pay);
    }
    return toResponse(sale);
  }

  @Transactional
  public CashierSaleResponse cancel(long saleId) {
    CashierSale sale = requireOpen(saleId);
    sale.setStatus("CANCELLED");
    sale.setUpdatedAt(LocalDateTime.now());
    saleMapper.updateById(sale);
    return toResponse(sale);
  }

  private void recalculate(CashierSale sale) {
    List<CashierSaleItem> items = listItems(sale.getId());
    int total = items.stream().mapToInt(i -> i.getAmountFen() == null ? 0 : i.getAmountFen()).sum();
    sale.setTotalFen(total);
    sale.setPayFen(Math.max(0, total - (sale.getDiscountFen() == null ? 0 : sale.getDiscountFen())));
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
