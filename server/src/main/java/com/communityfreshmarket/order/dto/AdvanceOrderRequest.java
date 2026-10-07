package com.communityfreshmarket.order.dto;

import jakarta.validation.constraints.NotBlank;

public class AdvanceOrderRequest {

  /** 期望当前状态，用于防连点。 */
  @NotBlank
  private String expectedStatus;

  public String getExpectedStatus() {
    return expectedStatus;
  }

  public void setExpectedStatus(String expectedStatus) {
    this.expectedStatus = expectedStatus;
  }
}
