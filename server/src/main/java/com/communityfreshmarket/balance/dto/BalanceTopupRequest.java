package com.communityfreshmarket.balance.dto;

public class BalanceTopupRequest {

  private String code;
  private Long checkinId;
  private Integer amountFen;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public Long getCheckinId() {
    return checkinId;
  }

  public void setCheckinId(Long checkinId) {
    this.checkinId = checkinId;
  }

  public Integer getAmountFen() {
    return amountFen;
  }

  public void setAmountFen(Integer amountFen) {
    this.amountFen = amountFen;
  }
}
