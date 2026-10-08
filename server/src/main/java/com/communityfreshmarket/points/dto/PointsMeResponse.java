package com.communityfreshmarket.points.dto;

import java.util.List;

public class PointsMeResponse {

  private int balance;
  private int pointsPerYuan;
  private List<PointLedgerItemResponse> recent;
  private List<PointRedeemResponse> redeems;
  private List<PointRedeemItemResponse> items;

  public PointsMeResponse(
      int balance,
      int pointsPerYuan,
      List<PointLedgerItemResponse> recent,
      List<PointRedeemResponse> redeems,
      List<PointRedeemItemResponse> items) {
    this.balance = balance;
    this.pointsPerYuan = pointsPerYuan;
    this.recent = recent;
    this.redeems = redeems;
    this.items = items;
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

  public List<PointRedeemItemResponse> getItems() {
    return items;
  }
}
