package com.communityfreshmarket.coupon.dto;

import com.communityfreshmarket.domain.Coupon;
import com.communityfreshmarket.domain.CouponClaim;
import java.time.LocalDateTime;

public class CouponClaimResponse {

  private long id;
  private long couponId;
  private String title;
  private String type;
  private int faceFen;
  private int minSpendFen;
  private String status;
  private LocalDateTime claimedAt;

  public static CouponClaimResponse from(CouponClaim claim, Coupon coupon) {
    CouponClaimResponse r = new CouponClaimResponse();
    r.id = claim.getId();
    r.couponId = claim.getCouponId();
    r.status = claim.getStatus();
    r.claimedAt = claim.getClaimedAt();
    if (coupon != null) {
      r.title = coupon.getTitle();
      r.type = coupon.getType();
      r.faceFen = coupon.getFaceFen() == null ? 0 : coupon.getFaceFen();
      r.minSpendFen = coupon.getMinSpendFen() == null ? 0 : coupon.getMinSpendFen();
    }
    return r;
  }

  public long getId() {
    return id;
  }

  public long getCouponId() {
    return couponId;
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

  public String getStatus() {
    return status;
  }

  public LocalDateTime getClaimedAt() {
    return claimedAt;
  }
}
