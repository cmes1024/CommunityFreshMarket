package com.communityfreshmarket.catalog;

import com.communityfreshmarket.common.BizException;
import com.communityfreshmarket.common.ErrorCodes;
import java.util.Locale;

public enum ProductBatchOp {
  ON_SHELF,
  OFF_SHELF,
  IN_STOCK,
  SOLD_OUT,
  RECOMMEND,
  UNRECOMMEND,
  DELETE;

  public static ProductBatchOp require(String code) {
    if (code == null || code.isBlank()) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "请选择操作");
    }
    try {
      return ProductBatchOp.valueOf(code.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "不支持的批量操作");
    }
  }
}
