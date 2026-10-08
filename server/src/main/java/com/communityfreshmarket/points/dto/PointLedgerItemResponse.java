package com.communityfreshmarket.points.dto;

import com.communityfreshmarket.domain.PointLedger;
import java.time.LocalDateTime;

public class PointLedgerItemResponse {

  private long id;
  private int delta;
  private int balanceAfter;
  private String reason;
  private String remark;
  private LocalDateTime createdAt;

  public static PointLedgerItemResponse from(PointLedger ledger) {
    PointLedgerItemResponse r = new PointLedgerItemResponse();
    r.id = ledger.getId();
    r.delta = ledger.getDelta() == null ? 0 : ledger.getDelta();
    r.balanceAfter = ledger.getBalanceAfter() == null ? 0 : ledger.getBalanceAfter();
    r.reason = ledger.getReason();
    r.remark = ledger.getRemark();
    r.createdAt = ledger.getCreatedAt();
    return r;
  }

  public long getId() {
    return id;
  }

  public int getDelta() {
    return delta;
  }

  public int getBalanceAfter() {
    return balanceAfter;
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
