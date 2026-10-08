package com.communityfreshmarket.checkin.dto;

import java.math.BigDecimal;

public class CheckinRequest {

  private BigDecimal lat;
  private BigDecimal lng;

  public BigDecimal getLat() {
    return lat;
  }

  public void setLat(BigDecimal lat) {
    this.lat = lat;
  }

  public BigDecimal getLng() {
    return lng;
  }

  public void setLng(BigDecimal lng) {
    this.lng = lng;
  }
}
