package com.communityfreshmarket.store;

import com.communityfreshmarket.common.BizException;
import com.communityfreshmarket.common.ErrorCodes;
import java.util.ArrayList;
import java.util.List;
import org.springframework.util.StringUtils;

/** 结算备注快捷短语：最多 8 句，每句最多 8 个字，库存换行分隔。 */
public final class RemarkPhrases {

  public static final int MAX_COUNT = 8;
  public static final int MAX_LENGTH = 8;

  private RemarkPhrases() {}

  public static List<String> parse(String stored) {
    if (!StringUtils.hasText(stored)) {
      return List.of();
    }
    List<String> out = new ArrayList<>();
    for (String line : stored.split("\n", -1)) {
      String text = line.trim();
      if (!StringUtils.hasText(text) || out.contains(text)) {
        continue;
      }
      if (text.length() > MAX_LENGTH) {
        text = text.substring(0, MAX_LENGTH);
      }
      out.add(text);
      if (out.size() == MAX_COUNT) {
        break;
      }
    }
    return List.copyOf(out);
  }

  public static String store(List<String> raw) {
    return String.join("\n", normalize(raw));
  }

  public static List<String> normalize(List<String> raw) {
    List<String> out = new ArrayList<>();
    if (raw == null) {
      return List.of();
    }
    for (String item : raw) {
      if (item == null) {
        continue;
      }
      String text = item.trim();
      if (!StringUtils.hasText(text)) {
        continue;
      }
      if (text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0) {
        throw new BizException(ErrorCodes.BAD_REQUEST, "备注短语不能换行");
      }
      if (text.length() > MAX_LENGTH) {
        throw new BizException(ErrorCodes.BAD_REQUEST, "每句备注最多 8 个字");
      }
      if (out.contains(text)) {
        continue;
      }
      if (out.size() >= MAX_COUNT) {
        throw new BizException(ErrorCodes.BAD_REQUEST, "备注短语最多 8 句");
      }
      out.add(text);
    }
    return List.copyOf(out);
  }
}
