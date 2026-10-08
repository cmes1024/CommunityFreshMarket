package com.communityfreshmarket.points.dto;

import com.communityfreshmarket.domain.PointRedeem;
import java.time.LocalDateTime;

public class PointRedeemResponse {

  private long id;
  private long userId;
  private int costPoints;
  private String title;
  private String status;
  private String nickname;
  private LocalDateTime createdAt;

  public static PointRedeemResponse from(PointRedeem redeem) {
    return from(redeem, null);
  }

  public static PointRedeemResponse from(PointRedeem redeem, String nickname) {
    PointRedeemResponse r = new PointRedeemResponse();
    r.id = redeem.getId();
    r.userId = redeem.getUserId();
    r.costPoints = redeem.getCostPoints() == null ? 0 : redeem.getCostPoints();
    r.title = redeem.getTitle();
    r.status = redeem.getStatus();
    r.nickname = nickname;
    r.createdAt = redeem.getCreatedAt();
    return r;
  }

  public long getId() {
    return id;
  }

  public long getUserId() {
    return userId;
  }

  public int getCostPoints() {
    return costPoints;
  }

  public String getTitle() {
    return title;
  }

  public String getStatus() {
    return status;
  }

  public String getNickname() {
    return nickname;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}
