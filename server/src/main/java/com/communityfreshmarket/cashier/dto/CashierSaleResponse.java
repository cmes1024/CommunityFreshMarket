package com.communityfreshmarket.cashier.dto;

import com.communityfreshmarket.domain.CashierSale;
import com.communityfreshmarket.domain.CashierSaleItem;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class CashierSaleResponse {

  private long id;
  private Long memberUserId;
  private String status;
  private int totalFen;
  private int discountFen;
  private int payFen;
  private String adjustReason;
  private LocalDateTime createdAt;
  private LocalDateTime completedAt;
  private List<Item> items;

  public static CashierSaleResponse from(CashierSale sale, List<CashierSaleItem> items) {
    CashierSaleResponse r = new CashierSaleResponse();
    r.id = sale.getId();
    r.memberUserId = sale.getMemberUserId();
    r.status = sale.getStatus();
    r.totalFen = sale.getTotalFen() == null ? 0 : sale.getTotalFen();
    r.discountFen = sale.getDiscountFen() == null ? 0 : sale.getDiscountFen();
    r.payFen = sale.getPayFen() == null ? 0 : sale.getPayFen();
    r.adjustReason = sale.getAdjustReason();
    r.createdAt = sale.getCreatedAt();
    r.completedAt = sale.getCompletedAt();
    r.items = items.stream().map(Item::from).toList();
    return r;
  }

  public long getId() {
    return id;
  }

  public Long getMemberUserId() {
    return memberUserId;
  }

  public String getStatus() {
    return status;
  }

  public int getTotalFen() {
    return totalFen;
  }

  public int getDiscountFen() {
    return discountFen;
  }

  public int getPayFen() {
    return payFen;
  }

  public String getAdjustReason() {
    return adjustReason;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getCompletedAt() {
    return completedAt;
  }

  public List<Item> getItems() {
    return items;
  }

  public static class Item {
    private long id;
    private Long productId;
    private String productName;
    private String unit;
    private int unitPriceFen;
    private BigDecimal qty;
    private int amountFen;
    private boolean adjusted;

    public static Item from(CashierSaleItem item) {
      Item r = new Item();
      r.id = item.getId();
      r.productId = item.getProductId();
      r.productName = item.getProductName();
      r.unit = item.getUnit();
      r.unitPriceFen = item.getUnitPriceFen() == null ? 0 : item.getUnitPriceFen();
      r.qty = item.getQty();
      r.amountFen = item.getAmountFen() == null ? 0 : item.getAmountFen();
      r.adjusted = Boolean.TRUE.equals(item.getAdjusted());
      return r;
    }

    public long getId() {
      return id;
    }

    public Long getProductId() {
      return productId;
    }

    public String getProductName() {
      return productName;
    }

    public String getUnit() {
      return unit;
    }

    public int getUnitPriceFen() {
      return unitPriceFen;
    }

    public BigDecimal getQty() {
      return qty;
    }

    public int getAmountFen() {
      return amountFen;
    }

    public boolean isAdjusted() {
      return adjusted;
    }
  }
}
