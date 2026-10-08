package com.communityfreshmarket.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("coupon")
public class Coupon {

  @TableId(type = IdType.AUTO)
  private Long id;
  private Long storeId;
  private String title;
  private String type;
  private Integer faceFen;
  private Integer minSpendFen;
  private Integer totalLimit;
  private Integer perUserLimit;
  private Integer claimedCount;
  private LocalDateTime startsAt;
  private LocalDateTime endsAt;
  private Boolean enabled;
  private Boolean removed;
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

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Integer getFaceFen() {
    return faceFen;
  }

  public void setFaceFen(Integer faceFen) {
    this.faceFen = faceFen;
  }

  public Integer getMinSpendFen() {
    return minSpendFen;
  }

  public void setMinSpendFen(Integer minSpendFen) {
    this.minSpendFen = minSpendFen;
  }

  public Integer getTotalLimit() {
    return totalLimit;
  }

  public void setTotalLimit(Integer totalLimit) {
    this.totalLimit = totalLimit;
  }

  public Integer getPerUserLimit() {
    return perUserLimit;
  }

  public void setPerUserLimit(Integer perUserLimit) {
    this.perUserLimit = perUserLimit;
  }

  public Integer getClaimedCount() {
    return claimedCount;
  }

  public void setClaimedCount(Integer claimedCount) {
    this.claimedCount = claimedCount;
  }

  public LocalDateTime getStartsAt() {
    return startsAt;
  }

  public void setStartsAt(LocalDateTime startsAt) {
    this.startsAt = startsAt;
  }

  public LocalDateTime getEndsAt() {
    return endsAt;
  }

  public void setEndsAt(LocalDateTime endsAt) {
    this.endsAt = endsAt;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public Boolean getRemoved() {
    return removed;
  }

  public void setRemoved(Boolean removed) {
    this.removed = removed;
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
