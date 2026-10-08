package com.communityfreshmarket.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("live_photo_request")
public class LivePhotoRequest {

  @TableId(type = IdType.AUTO)
  private Long id;
  private Long storeId;
  private Long productId;
  private Integer applicantCount;
  private LocalDateTime lastRequestedAt;
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

  public Long getProductId() {
    return productId;
  }

  public void setProductId(Long productId) {
    this.productId = productId;
  }

  public Integer getApplicantCount() {
    return applicantCount;
  }

  public void setApplicantCount(Integer applicantCount) {
    this.applicantCount = applicantCount;
  }

  public LocalDateTime getLastRequestedAt() {
    return lastRequestedAt;
  }

  public void setLastRequestedAt(LocalDateTime lastRequestedAt) {
    this.lastRequestedAt = lastRequestedAt;
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
