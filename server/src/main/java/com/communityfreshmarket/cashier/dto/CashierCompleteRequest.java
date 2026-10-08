package com.communityfreshmarket.cashier.dto;

public class CashierCompleteRequest {

  private Integer discountFen;
  private String adjustReason;

  public Integer getDiscountFen() {
    return discountFen;
  }

  public void setDiscountFen(Integer discountFen) {
    this.discountFen = discountFen;
  }

  public String getAdjustReason() {
    return adjustReason;
  }

  public void setAdjustReason(String adjustReason) {
    this.adjustReason = adjustReason;
  }
}
