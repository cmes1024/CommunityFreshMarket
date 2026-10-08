package com.communityfreshmarket.coupon.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public class CouponSaveRequest {

  @NotBlank
  @Size(max = 64)
  private String title;

  private String type;

  @NotNull
  @Min(1)
  private Integer faceFen;

  @Min(0)
  private Integer minSpendFen;

  private Integer totalLimit;
  private Integer perUserLimit;
  private LocalDateTime startsAt;
  private LocalDateTime endsAt;
  private Boolean enabled;

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
}
