package com.communityfreshmarket.points.dto;

import com.communityfreshmarket.domain.PointRedeemItem;

public class PointRedeemItemResponse {

  private long id;
  private Long productId;
  private String title;
  private int costPoints;
  private boolean enabled;

  public static PointRedeemItemResponse from(PointRedeemItem item) {
    PointRedeemItemResponse r = new PointRedeemItemResponse();
    r.id = item.getId();
    r.productId = item.getProductId();
    r.title = item.getTitle();
    r.costPoints = item.getCostPoints() == null ? 0 : item.getCostPoints();
    r.enabled = Boolean.TRUE.equals(item.getEnabled());
    return r;
  }

  public long getId() {
    return id;
  }

  public Long getProductId() {
    return productId;
  }

  public String getTitle() {
    return title;
  }

  public int getCostPoints() {
    return costPoints;
  }

  public boolean isEnabled() {
    return enabled;
  }
}
