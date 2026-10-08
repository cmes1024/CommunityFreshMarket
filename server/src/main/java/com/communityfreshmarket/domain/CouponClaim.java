package com.communityfreshmarket.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("coupon_claim")
public class CouponClaim {

  @TableId(type = IdType.AUTO)
  private Long id;
  private Long couponId;
  private Long storeId;
  private Long userId;
  private String status;
  private Long orderId;
  private Long cashierId;
  private LocalDateTime claimedAt;
  private LocalDateTime usedAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getCouponId() {
    return couponId;
  }

  public void setCouponId(Long couponId) {
    this.couponId = couponId;
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

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Long getOrderId() {
    return orderId;
  }

  public void setOrderId(Long orderId) {
    this.orderId = orderId;
  }

  public Long getCashierId() {
    return cashierId;
  }

  public void setCashierId(Long cashierId) {
    this.cashierId = cashierId;
  }

  public LocalDateTime getClaimedAt() {
    return claimedAt;
  }

  public void setClaimedAt(LocalDateTime claimedAt) {
    this.claimedAt = claimedAt;
  }

  public LocalDateTime getUsedAt() {
    return usedAt;
  }

  public void setUsedAt(LocalDateTime usedAt) {
    this.usedAt = usedAt;
  }
}
