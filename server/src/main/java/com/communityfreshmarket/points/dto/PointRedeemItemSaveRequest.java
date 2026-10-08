package com.communityfreshmarket.points.dto;

public class PointRedeemItemSaveRequest {

  private Long productId;
  private String title;
  private Integer costPoints;
  private Boolean enabled;

  public Long getProductId() {
    return productId;
  }

  public void setProductId(Long productId) {
    this.productId = productId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Integer getCostPoints() {
    return costPoints;
  }

  public void setCostPoints(Integer costPoints) {
    this.costPoints = costPoints;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }
}
