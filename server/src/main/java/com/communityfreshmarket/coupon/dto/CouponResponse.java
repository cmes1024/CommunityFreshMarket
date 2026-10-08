package com.communityfreshmarket.coupon.dto;

import com.communityfreshmarket.domain.Coupon;
import java.time.LocalDateTime;

public class CouponResponse {

  private long id;
  private String title;
  private String type;
  private int faceFen;
  private int minSpendFen;
  private Integer totalLimit;
  private int perUserLimit;
  private int claimedCount;
  private LocalDateTime startsAt;
  private LocalDateTime endsAt;
  private boolean enabled;

  public static CouponResponse from(Coupon c) {
    CouponResponse r = new CouponResponse();
    r.id = c.getId();
    r.title = c.getTitle();
    r.type = c.getType();
    r.faceFen = c.getFaceFen() == null ? 0 : c.getFaceFen();
    r.minSpendFen = c.getMinSpendFen() == null ? 0 : c.getMinSpendFen();
    r.totalLimit = c.getTotalLimit();
    r.perUserLimit = c.getPerUserLimit() == null ? 1 : c.getPerUserLimit();
    r.claimedCount = c.getClaimedCount() == null ? 0 : c.getClaimedCount();
    r.startsAt = c.getStartsAt();
    r.endsAt = c.getEndsAt();
    r.enabled = Boolean.TRUE.equals(c.getEnabled());
    return r;
  }

  public long getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getType() {
    return type;
  }

  public int getFaceFen() {
    return faceFen;
  }

  public int getMinSpendFen() {
    return minSpendFen;
  }

  public Integer getTotalLimit() {
    return totalLimit;
  }

  public int getPerUserLimit() {
    return perUserLimit;
  }

  public int getClaimedCount() {
    return claimedCount;
  }

  public LocalDateTime getStartsAt() {
    return startsAt;
  }

  public LocalDateTime getEndsAt() {
    return endsAt;
  }

  public boolean isEnabled() {
    return enabled;
  }
}
