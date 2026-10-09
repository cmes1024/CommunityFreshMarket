package com.communityfreshmarket.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("`order`")
public class Order {

  @TableId(type = IdType.AUTO)
  private Long id;
  private Long storeId;
  private Long userId;
  private String fulfillment;
  private String status;
  private Boolean asap;
  private String remark;
  private String addressSnapshot;
  private Long pickupSlotId;
  private String pickupSlotLabel;
  private Integer totalAmountFen;
  private Long couponClaimId;
  private Integer discountFen;
  private Integer balanceFen;
  private Boolean merchantUnread;
  private LocalDateTime customerArrivedAt;
  private String cancelBy;
  private String cancelReason;
  private LocalDateTime cancelledAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getStoreId() {
    return storeId;
  }

  public void setStoreId(Long storeId) {
    this.storeId = storeId;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getFulfillment() {
    return fulfillment;
  }

  public void setFulfillment(String fulfillment) {
    this.fulfillment = fulfillment;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
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

  public String getAddressSnapshot() {
    return addressSnapshot;
  }

  public void setAddressSnapshot(String addressSnapshot) {
    this.addressSnapshot = addressSnapshot;
  }

  public Long getPickupSlotId() {
    return pickupSlotId;
  }

  public void setPickupSlotId(Long pickupSlotId) {
    this.pickupSlotId = pickupSlotId;
  }

  public String getPickupSlotLabel() {
    return pickupSlotLabel;
  }

  public void setPickupSlotLabel(String pickupSlotLabel) {
    this.pickupSlotLabel = pickupSlotLabel;
  }

  public Integer getTotalAmountFen() {
    return totalAmountFen;
  }

  public void setTotalAmountFen(Integer totalAmountFen) {
    this.totalAmountFen = totalAmountFen;
  }

  public Long getCouponClaimId() {
    return couponClaimId;
  }

  public void setCouponClaimId(Long couponClaimId) {
    this.couponClaimId = couponClaimId;
  }

  public Integer getDiscountFen() {
    return discountFen;
  }

  public void setDiscountFen(Integer discountFen) {
    this.discountFen = discountFen;
  }

  public Integer getBalanceFen() {
    return balanceFen;
  }

  public void setBalanceFen(Integer balanceFen) {
    this.balanceFen = balanceFen;
  }

  public Boolean getMerchantUnread() {
    return merchantUnread;
  }

  public void setMerchantUnread(Boolean merchantUnread) {
    this.merchantUnread = merchantUnread;
  }

  public LocalDateTime getCustomerArrivedAt() {
    return customerArrivedAt;
  }

  public void setCustomerArrivedAt(LocalDateTime customerArrivedAt) {
    this.customerArrivedAt = customerArrivedAt;
  }

  public String getCancelBy() {
    return cancelBy;
  }

  public void setCancelBy(String cancelBy) {
    this.cancelBy = cancelBy;
  }

  public String getCancelReason() {
    return cancelReason;
  }

  public void setCancelReason(String cancelReason) {
    this.cancelReason = cancelReason;
  }

  public LocalDateTime getCancelledAt() {
    return cancelledAt;
  }

  public void setCancelledAt(LocalDateTime cancelledAt) {
    this.cancelledAt = cancelledAt;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
