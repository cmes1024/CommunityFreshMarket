package com.communityfreshmarket.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("balance_ledger")
public class BalanceLedger {

  @TableId(type = IdType.AUTO)
  private Long id;
  private Long storeId;
  private Long userId;
  private Integer deltaFen;
  private Integer balanceAfterFen;
  private String reason;
  private String refType;
  private Long refId;
  private Long operatorUserId;
  private String remark;
  private LocalDateTime createdAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getStoreId() {
    return storeId;
  }

  public void setStoreId(Long storeId) {
    this.storeId = storeId;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Integer getDeltaFen() {
    return deltaFen;
  }

  public void setDeltaFen(Integer deltaFen) {
    this.deltaFen = deltaFen;
  }

  public Integer getBalanceAfterFen() {
    return balanceAfterFen;
  }

  public void setBalanceAfterFen(Integer balanceAfterFen) {
    this.balanceAfterFen = balanceAfterFen;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public String getRefType() {
    return refType;
  }

  public void setRefType(String refType) {
    this.refType = refType;
  }

  public Long getRefId() {
    return refId;
  }

  public void setRefId(Long refId) {
    this.refId = refId;
  }

  public Long getOperatorUserId() {
    return operatorUserId;
  }

  public void setOperatorUserId(Long operatorUserId) {
    this.operatorUserId = operatorUserId;
  }

  public String getRemark() {
    return remark;
  }

  public void setRemark(String remark) {
    this.remark = remark;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
