package com.communityfreshmarket.points.dto;

import java.util.List;

public class PointsMeResponse {

  private int balance;
  private int pointsPerYuan;
  private List<PointLedgerItemResponse> recent;
  private List<PointRedeemResponse> redeems;

  public PointsMeResponse(
      int balance,
      int pointsPerYuan,
      List<PointLedgerItemResponse> recent,
      List<PointRedeemResponse> redeems) {
    this.balance = balance;
    this.pointsPerYuan = pointsPerYuan;
    this.recent = recent;
    this.redeems = redeems;
  }

  public int getBalance() {
    return balance;
  }

  public int getPointsPerYuan() {
    return pointsPerYuan;
  }

  public List<PointLedgerItemResponse> getRecent() {
    return recent;
  }

  public List<PointRedeemResponse> getRedeems() {
    return redeems;
  }
}
