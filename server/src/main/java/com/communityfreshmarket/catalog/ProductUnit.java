package com.communityfreshmarket.catalog;

import com.communityfreshmarket.common.BizException;
import com.communityfreshmarket.common.ErrorCodes;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public enum ProductUnit {
  PIECE("件"),
  PORTION("份"),
  JIN("斤");

  private final String label;

  ProductUnit(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

  public static ProductUnit require(String code) {
    if (code == null || code.isBlank()) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "单位不能为空");
    }
    try {
      return ProductUnit.valueOf(code.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "单位无效，仅支持件/份/斤");
    }
  }

  public static Set<String> codes() {
    return Arrays.stream(values()).map(Enum::name).collect(Collectors.toSet());
  }
}
