package com.communityfreshmarket.points.dto;

public class CounterPointResponse {

  private long userId;
  private String nickname;
  private int amountFen;
  private int points;
  private int balance;

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

  public int getPoints() {
    return points;
  }

  public void setPoints(int points) {
    this.points = points;
  }

  public int getBalance() {
    return balance;
  }

  public void setBalance(int balance) {
    this.balance = balance;
  }
}
