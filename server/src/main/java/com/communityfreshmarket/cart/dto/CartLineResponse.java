package com.communityfreshmarket.cart.dto;

import java.math.BigDecimal;

public class CartLineResponse {

  private long productId;
  private String name;
  private String imageUrl;
  private String unit;
  private String unitLabel;
  private int priceFen;
  private BigDecimal qty;
  private int amountFen;
  private boolean available;
  private String unavailableReason;

  public long getProductId() {
    return productId;
  }

  public void setProductId(long productId) {
    this.productId = productId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  public String getUnit() {
    return unit;
  }

  public void setUnit(String unit) {
    this.unit = unit;
  }

  public String getUnitLabel() {
    return unitLabel;
  }

  public void setUnitLabel(String unitLabel) {
    this.unitLabel = unitLabel;
  }

  public int getPriceFen() {
    return priceFen;
  }

  public void setPriceFen(int priceFen) {
    this.priceFen = priceFen;
  }

  public BigDecimal getQty() {
    return qty;
  }

  public void setQty(BigDecimal qty) {
    this.qty = qty;
  }

  public int getAmountFen() {
    return amountFen;
  }

  public void setAmountFen(int amountFen) {
    this.amountFen = amountFen;
  }

  public boolean isAvailable() {
    return available;
  }

  public void setAvailable(boolean available) {
    this.available = available;
  }

  public String getUnavailableReason() {
    return unavailableReason;
  }

  public void setUnavailableReason(String unavailableReason) {
    this.unavailableReason = unavailableReason;
  }
}
