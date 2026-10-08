package com.communityfreshmarket.catalog;

import com.communityfreshmarket.common.BizException;
import com.communityfreshmarket.common.ErrorCodes;
import com.communityfreshmarket.domain.Product;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** 起购 / 限购 / 库存上限校验（第二波）。 */
public final class ProductQtyRules {

  private static final BigDecimal HARD_MAX = new BigDecimal("999");

  private ProductQtyRules() {}

  public static void validateUnitQty(String unit, BigDecimal qty) {
    if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "数量须大于 0");
    }
    if (qty.compareTo(HARD_MAX) > 0) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "数量过大");
    }
    ProductUnit u = ProductUnit.require(unit);
    if (u == ProductUnit.JIN) {
      if (qty.scale() > 2) {
        throw new BizException(ErrorCodes.BAD_REQUEST, "斤最多两位小数");
      }
    } else if (qty.stripTrailingZeros().scale() > 0) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "件/份须为整数");
    }
  }

  /** 校验目标篮数量（含起购/限购/库存）。 */
  public static void validatePurchasableQty(Product product, BigDecimal qty) {
    validateUnitQty(product.getUnit(), qty);
    String unit = ProductUnit.require(product.getUnit()).getLabel();
    BigDecimal min = product.getMinQty();
    if (min != null && qty.compareTo(min) < 0) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "至少购买 " + format(min) + " " + unit);
    }
    BigDecimal cap = effectiveMax(product);
    if (cap != null && qty.compareTo(cap) > 0) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "最多购买 " + format(cap) + " " + unit);
    }
  }

  /** 限购与库存取更严；皆空则仅 HARD_MAX。 */
  public static BigDecimal effectiveMax(Product product) {
    BigDecimal max = product.getMaxQty();
    BigDecimal stock = product.getStockQty();
    if (max == null && stock == null) {
      return HARD_MAX;
    }
    if (max == null) {
      return stock.min(HARD_MAX);
    }
    if (stock == null) {
      return max.min(HARD_MAX);
    }
    return max.min(stock).min(HARD_MAX);
  }

  /** 未配置起购时返回 null（前端详情默认 1，列表默认 0） */
  public static BigDecimal effectiveMin(Product product) {
    return product.getMinQty();
  }

  public static void validateLimitsConfig(BigDecimal minQty, BigDecimal maxQty, BigDecimal stockQty) {
    if (minQty != null && minQty.compareTo(BigDecimal.ZERO) < 0) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "起购不能为负");
    }
    if (maxQty != null && maxQty.compareTo(BigDecimal.ZERO) <= 0) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "限购须大于 0");
    }
    if (stockQty != null && stockQty.compareTo(BigDecimal.ZERO) < 0) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "库存不能为负");
    }
    if (minQty != null && maxQty != null && minQty.compareTo(maxQty) > 0) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "起购不能大于限购");
    }
  }

  public static String format(BigDecimal qty) {
    if (qty == null) {
      return "";
    }
    return qty.stripTrailingZeros().toPlainString();
  }

  public static BigDecimal normalizeStock(BigDecimal qty) {
    if (qty == null) {
      return null;
    }
    return qty.setScale(2, RoundingMode.HALF_UP);
  }
}
