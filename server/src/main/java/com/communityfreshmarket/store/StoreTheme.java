package com.communityfreshmarket.store;

import com.communityfreshmarket.common.BizException;
import com.communityfreshmarket.common.ErrorCodes;
import java.util.Set;
import java.util.regex.Pattern;

/** 门店主题。色板在小程序生成，这里只校验预设名和自定义主色。 */
public final class StoreTheme {

  public static final String PRODUCE = "PRODUCE";
  public static final String CUSTOM = "CUSTOM";

  private static final Set<String> PRESETS =
      Set.of("PRODUCE", "BREAKFAST", "EATERY", "MARKET", "CUSTOM");
  private static final Pattern HEX = Pattern.compile("#[0-9a-f]{6}");

  private StoreTheme() {}

  public static String readPreset(String raw) {
    if (raw == null || raw.isBlank()) return PRODUCE;
    String mapped = map(raw.trim().toUpperCase());
    return mapped == null ? PRODUCE : mapped;
  }

  /** 非自定义，或色值不合法时返回 null。 */
  public static String readPrimary(String preset, String raw) {
    if (!CUSTOM.equals(preset) || raw == null) return null;
    String value = raw.trim().toLowerCase();
    return HEX.matcher(value).matches() ? value : null;
  }

  public static String preset(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "请选择主题");
    }
    String mapped = map(raw.trim().toUpperCase());
    if (mapped == null) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "主题仅支持青绿、赭石、绛红、海蓝或自定义");
    }
    return mapped;
  }

  public static String primary(String preset, String raw) {
    if (!CUSTOM.equals(preset)) return null;
    String value = raw == null ? "" : raw.trim().toLowerCase();
    if (!HEX.matcher(value).matches()) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "请选择主色");
    }
    return value;
  }

  /** 旧名 TEAL / AMBER / BRICK / INK 读出来改成行业预设。无法识别时返回 null。 */
  private static String map(String value) {
    return switch (value) {
      case "PRODUCE", "TEAL" -> PRODUCE;
      case "BREAKFAST", "AMBER" -> "BREAKFAST";
      case "EATERY", "BRICK" -> "EATERY";
      case "MARKET", "INK" -> "MARKET";
      case "CUSTOM" -> CUSTOM;
      default -> null;
    };
  }
}
