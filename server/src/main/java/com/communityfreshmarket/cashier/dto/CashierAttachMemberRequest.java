package com.communityfreshmarket.cashier.dto;

public class CashierAttachMemberRequest {

  private Long memberUserId;
  private Long checkinId;

  public Long getMemberUserId() {
    return memberUserId;
  }

  public void setMemberUserId(Long memberUserId) {
    this.memberUserId = memberUserId;
  }

  public Long getCheckinId() {
    return checkinId;
  }

  public void setCheckinId(Long checkinId) {
    this.checkinId = checkinId;
  }
}
