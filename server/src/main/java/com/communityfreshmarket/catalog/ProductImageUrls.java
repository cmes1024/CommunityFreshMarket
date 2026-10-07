package com.communityfreshmarket.catalog;

import com.communityfreshmarket.common.BizException;
import com.communityfreshmarket.common.ErrorCodes;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.util.StringUtils;

/** 商品图：最多 3 张；封面 = 第一张。 */
public final class ProductImageUrls {

  public static final int MAX = 3;

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private ProductImageUrls() {}

  public static List<String> resolve(String imageUrlsJson, String imageUrl) {
    List<String> fromJson = parse(imageUrlsJson);
    if (!fromJson.isEmpty()) {
      return fromJson;
    }
    if (StringUtils.hasText(imageUrl)) {
      return List.of(imageUrl.trim());
    }
    return List.of();
  }

  /** 校验并规范化；超过上限抛业务异常。 */
  public static List<String> normalize(List<String> raw) {
    if (raw == null || raw.isEmpty()) {
      return List.of();
    }
    LinkedHashSet<String> seen = new LinkedHashSet<>();
    for (String item : raw) {
      if (!StringUtils.hasText(item)) {
        continue;
      }
      String url = item.trim();
      if (url.length() > 512) {
        throw new BizException(ErrorCodes.BAD_REQUEST, "图片地址过长");
      }
      seen.add(url);
      if (seen.size() > MAX) {
        throw new BizException(ErrorCodes.BAD_REQUEST, "图片最多 " + MAX + " 张");
      }
    }
    return new ArrayList<>(seen);
  }

  /** 兼容旧客户端：优先 imageUrls，否则单张 imageUrl。 */
  public static List<String> fromRequest(List<String> imageUrls, String imageUrl) {
    if (imageUrls != null) {
      return normalize(imageUrls);
    }
    if (StringUtils.hasText(imageUrl)) {
      return normalize(List.of(imageUrl));
    }
    return List.of();
  }

  public static String cover(List<String> urls) {
    return urls == null || urls.isEmpty() ? null : urls.get(0);
  }

  public static String toJson(List<String> urls) {
    if (urls == null || urls.isEmpty()) {
      return null;
    }
    try {
      return MAPPER.writeValueAsString(urls);
    } catch (Exception ex) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "图片数据无效");
    }
  }

  private static List<String> parse(String json) {
    if (!StringUtils.hasText(json)) {
      return Collections.emptyList();
    }
    try {
      List<String> list = MAPPER.readValue(json, new TypeReference<>() {});
      if (list == null || list.isEmpty()) {
        return Collections.emptyList();
      }
      List<String> out = new ArrayList<>();
      for (String item : list) {
        if (StringUtils.hasText(item)) {
          out.add(item.trim());
        }
        if (out.size() >= MAX) {
          break;
        }
      }
      return out;
    } catch (Exception ex) {
      return Collections.emptyList();
    }
  }
}
