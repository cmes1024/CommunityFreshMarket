package com.communityfreshmarket.balance;

/** 订单只通过这里扣余额、退余额。 */
public interface OrderBalance {

  /** @return 实际抵扣的分；不用余额或余额为 0 时返回 0 */
  int apply(long orderId, long userId, long storeId, int payableFen, boolean use);

  void refund(long orderId, long userId, long storeId);
}
