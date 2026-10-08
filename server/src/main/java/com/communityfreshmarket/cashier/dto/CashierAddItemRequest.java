package com.communityfreshmarket.cashier.dto;

import java.math.BigDecimal;

public class CashierAddItemRequest {

  private Long productId;
  private BigDecimal qty;
  private Integer unitPriceFen;

  public Long getProductId() {
    return productId;
  }

  public void setProductId(Long productId) {
    this.productId = productId;
  }

  public BigDecimal getQty() {
    return qty;
  }

  public void setQty(BigDecimal qty) {
    this.qty = qty;
  }

  public Integer getUnitPriceFen() {
    return unitPriceFen;
  }

  public void setUnitPriceFen(Integer unitPriceFen) {
    this.unitPriceFen = unitPriceFen;
  }
}
