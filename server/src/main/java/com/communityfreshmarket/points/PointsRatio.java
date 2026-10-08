package com.communityfreshmarket.points;

import com.communityfreshmarket.common.BizException;
import com.communityfreshmarket.common.ErrorCodes;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** 每元积分：0 关闭，最多两位小数，上限 100。入账按整元向下取整后再乘比例，积分仍记整数。 */
public final class PointsRatio {

  public static final BigDecimal MAX = new BigDecimal("100");

  private PointsRatio() {}

  public static BigDecimal require(BigDecimal ratio) {
    if (ratio == null) {
      return null;
    }
    if (ratio.signum() < 0 || ratio.compareTo(MAX) > 0) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "每元积分需在 0 到 100 之间");
    }
    try {
      ratio.setScale(2, RoundingMode.UNNECESSARY);
    } catch (ArithmeticException ex) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "每元积分最多两位小数");
    }
    return plain(ratio.setScale(2, RoundingMode.UNNECESSARY));
  }

  public static BigDecimal plain(BigDecimal ratio) {
    if (ratio == null) {
      return null;
    }
    return new BigDecimal(ratio.stripTrailingZeros().toPlainString());
  }

  public static int earn(int amountFen, BigDecimal ratio) {
    if (ratio == null || ratio.signum() <= 0 || amountFen < 100) {
      return 0;
    }
    int yuan = amountFen / 100;
    return ratio.multiply(BigDecimal.valueOf(yuan)).setScale(0, RoundingMode.DOWN).intValue();
  }
}
