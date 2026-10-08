package com.communityfreshmarket.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("cashier_sale")
public class CashierSale {

  @TableId(type = IdType.AUTO)
  private Long id;
  private Long storeId;
  private Long memberUserId;
  private String status;
  private Integer totalFen;
  private Integer discountFen;
  private Integer payFen;
  private String adjustReason;
  private Long createdBy;
  private LocalDateTime completedAt;
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

  public Long getMemberUserId() {
    return memberUserId;
  }

  public void setMemberUserId(Long memberUserId) {
    this.memberUserId = memberUserId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Integer getTotalFen() {
    return totalFen;
  }

  public void setTotalFen(Integer totalFen) {
    this.totalFen = totalFen;
  }

  public Integer getDiscountFen() {
    return discountFen;
  }

  public void setDiscountFen(Integer discountFen) {
    this.discountFen = discountFen;
  }

  public Integer getPayFen() {
    return payFen;
  }

  public void setPayFen(Integer payFen) {
    this.payFen = payFen;
  }

  public String getAdjustReason() {
    return adjustReason;
  }

  public void setAdjustReason(String adjustReason) {
    this.adjustReason = adjustReason;
  }

  public Long getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(Long createdBy) {
    this.createdBy = createdBy;
  }

  public LocalDateTime getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(LocalDateTime completedAt) {
    this.completedAt = completedAt;
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
