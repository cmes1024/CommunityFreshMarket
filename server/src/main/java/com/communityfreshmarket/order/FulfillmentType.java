package com.communityfreshmarket.order;

import com.communityfreshmarket.common.BizException;
import com.communityfreshmarket.common.ErrorCodes;

public enum FulfillmentType {
  DELIVERY,
  PICKUP;

  public static FulfillmentType require(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "请选择配送或自提");
    }
    try {
      return FulfillmentType.valueOf(raw.trim().toUpperCase());
    } catch (Exception ex) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "履约方式无效");
    }
  }
}
