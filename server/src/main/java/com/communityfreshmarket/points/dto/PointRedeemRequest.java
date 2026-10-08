package com.communityfreshmarket.points.dto;

import jakarta.validation.constraints.NotNull;

public class PointRedeemRequest {

  @NotNull
  private Long itemId;

  public Long getItemId() {
    return itemId;
  }

  public void setItemId(Long itemId) {
    this.itemId = itemId;
  }
}
