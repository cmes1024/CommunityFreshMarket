package com.communityfreshmarket.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("balance_account")
public class BalanceAccount {

  private Long userId;
  private Long storeId;
  private Integer balanceFen;
  private LocalDateTime updatedAt;

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Long getStoreId() {
    return storeId;
  }

  public void setStoreId(Long storeId) {
    this.storeId = storeId;
  }

  public Integer getBalanceFen() {
    return balanceFen;
  }

  public void setBalanceFen(Integer balanceFen) {
    this.balanceFen = balanceFen;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
