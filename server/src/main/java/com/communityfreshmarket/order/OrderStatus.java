package com.communityfreshmarket.order;

import com.communityfreshmarket.common.BizException;
import com.communityfreshmarket.common.ErrorCodes;
import java.util.Set;

public enum OrderStatus {
  PENDING_CONFIRM,
  PREPARING,
  DELIVERING,
  READY_PICKUP,
  COMPLETED,
  CANCELLED;

  public static final Set<OrderStatus> UNFINISHED =
      Set.of(PENDING_CONFIRM, PREPARING, DELIVERING, READY_PICKUP);

  public static OrderStatus require(String raw) {
    try {
      return OrderStatus.valueOf(raw);
    } catch (Exception ex) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "订单状态无效");
    }
  }

  public boolean unfinished() {
    return UNFINISHED.contains(this);
  }

  public String label(String fulfillment) {
    return switch (this) {
      case PENDING_CONFIRM -> "待确认";
      case PREPARING -> "备货中";
      case DELIVERING -> "配送中";
      case READY_PICKUP -> "待自提";
      case COMPLETED -> "已完成";
      case CANCELLED -> "已取消";
    };
  }

  public static OrderStatus next(OrderStatus current, String fulfillment) {
    return switch (current) {
      case PENDING_CONFIRM -> PREPARING;
      case PREPARING -> "PICKUP".equals(fulfillment) ? READY_PICKUP : DELIVERING;
      case DELIVERING, READY_PICKUP -> COMPLETED;
      default -> throw new BizException(ErrorCodes.BAD_REQUEST, "当前状态不可推进");
    };
  }
}
