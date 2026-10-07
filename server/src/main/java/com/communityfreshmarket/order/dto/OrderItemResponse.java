package com.communityfreshmarket.order.dto;

import com.communityfreshmarket.catalog.ProductUnit;
import com.communityfreshmarket.domain.OrderItem;
import java.math.BigDecimal;

public class OrderItemResponse {

  private Long productId;
  private String productName;
  private String unit;
  private String unitLabel;
  private int unitPriceFen;
  private BigDecimal qty;
  private int amountFen;

  public static OrderItemResponse from(OrderItem item) {
    OrderItemResponse r = new OrderItemResponse();
    r.productId = item.getProductId();
    r.productName = item.getProductName();
    r.unit = item.getUnit();
    try {
      r.unitLabel = ProductUnit.valueOf(item.getUnit()).getLabel();
    } catch (Exception ex) {
      r.unitLabel = item.getUnit();
    }
    r.unitPriceFen = item.getUnitPriceFen() == null ? 0 : item.getUnitPriceFen();
    r.qty = item.getQty();
    r.amountFen = item.getAmountFen() == null ? 0 : item.getAmountFen();
    return r;
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

  public String getUnitLabel() {
    return unitLabel;
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
}
