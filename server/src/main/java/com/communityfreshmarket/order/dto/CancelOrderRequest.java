package com.communityfreshmarket.order.dto;

import jakarta.validation.constraints.Size;

public class CancelOrderRequest {

  @Size(max = 255)
  private String reason;

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }
}
