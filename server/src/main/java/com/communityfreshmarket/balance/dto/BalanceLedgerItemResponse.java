package com.communityfreshmarket.balance.dto;

import com.communityfreshmarket.domain.BalanceLedger;
import java.time.LocalDateTime;

public class BalanceLedgerItemResponse {

  private long id;
  private int deltaFen;
  private int balanceAfterFen;
  private String reason;
  private String remark;
  private LocalDateTime createdAt;

  public static BalanceLedgerItemResponse from(BalanceLedger ledger) {
    BalanceLedgerItemResponse r = new BalanceLedgerItemResponse();
    r.id = ledger.getId();
    r.deltaFen = ledger.getDeltaFen() == null ? 0 : ledger.getDeltaFen();
    r.balanceAfterFen = ledger.getBalanceAfterFen() == null ? 0 : ledger.getBalanceAfterFen();
    r.reason = ledger.getReason();
    r.remark = ledger.getRemark();
    r.createdAt = ledger.getCreatedAt();
    return r;
  }

  public long getId() {
    return id;
  }

  public int getDeltaFen() {
    return deltaFen;
  }

  public int getBalanceAfterFen() {
    return balanceAfterFen;
  }

  public String getReason() {
    return reason;
  }

  public String getRemark() {
    return remark;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}
