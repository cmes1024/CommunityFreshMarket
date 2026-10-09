package com.communityfreshmarket.balance.dto;

import java.util.List;

public class BalanceMeResponse {

  private int balanceFen;
  private List<BalanceLedgerItemResponse> recent;

  public BalanceMeResponse(int balanceFen, List<BalanceLedgerItemResponse> recent) {
    this.balanceFen = balanceFen;
    this.recent = recent;
  }

  public int getBalanceFen() {
    return balanceFen;
  }

  public List<BalanceLedgerItemResponse> getRecent() {
    return recent;
  }
}
