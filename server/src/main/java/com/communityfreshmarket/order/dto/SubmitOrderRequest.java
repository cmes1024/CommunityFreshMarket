package com.communityfreshmarket.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SubmitOrderRequest {

  @NotBlank
  private String fulfillment;

  private Long addressId;
  private Long pickupSlotId;
  private Boolean asap;

  @Size(max = 512)
  private String remark;

  private Long couponClaimId;
  private Boolean useBalance;

  public String getFulfillment() {
    return fulfillment;
  }

  public void setFulfillment(String fulfillment) {
    this.fulfillment = fulfillment;
  }

  public Long getAddressId() {
    return addressId;
  }

  public void setAddressId(Long addressId) {
    this.addressId = addressId;
  }

  public Long getPickupSlotId() {
    return pickupSlotId;
  }

  public void setPickupSlotId(Long pickupSlotId) {
    this.pickupSlotId = pickupSlotId;
  }

  public Boolean getAsap() {
    return asap;
  }

  public void setAsap(Boolean asap) {
    this.asap = asap;
  }

  public String getRemark() {
    return remark;
  }

  public void setRemark(String remark) {
    this.remark = remark;
  }

  public Long getCouponClaimId() {
    return couponClaimId;
  }

  public void setCouponClaimId(Long couponClaimId) {
    this.couponClaimId = couponClaimId;
  }

  public Boolean getUseBalance() {
    return useBalance;
  }

  public void setUseBalance(Boolean useBalance) {
    this.useBalance = useBalance;
  }
}
