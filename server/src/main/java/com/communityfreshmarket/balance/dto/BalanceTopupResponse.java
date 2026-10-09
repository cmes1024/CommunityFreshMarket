package com.communityfreshmarket.balance.dto;

public class BalanceTopupResponse {

  private long userId;
  private String nickname;
  private int amountFen;
  private int balanceFen;

  public long getUserId() {
    return userId;
  }

  public void setUserId(long userId) {
    this.userId = userId;
  }

  public String getNickname() {
    return nickname;
  }

  public void setNickname(String nickname) {
    this.nickname = nickname;
  }

  public int getAmountFen() {
    return amountFen;
  }

  public void setAmountFen(int amountFen) {
    this.amountFen = amountFen;
  }

  public int getBalanceFen() {
    return balanceFen;
  }

  public void setBalanceFen(int balanceFen) {
    this.balanceFen = balanceFen;
  }
}
