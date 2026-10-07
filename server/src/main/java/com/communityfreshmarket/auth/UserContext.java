package com.communityfreshmarket.auth;

public final class UserContext {

  private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
  private static final ThreadLocal<Long> STORE_ID = new ThreadLocal<>();

  private UserContext() {}

  public static void set(Long userId, Long storeId) {
    USER_ID.set(userId);
    STORE_ID.set(storeId);
  }

  public static Long userId() {
    return USER_ID.get();
  }

  public static Long storeId() {
    return STORE_ID.get();
  }

  public static long requireUserId() {
    Long id = USER_ID.get();
    if (id == null) {
      throw new com.communityfreshmarket.common.BizException(
          com.communityfreshmarket.common.ErrorCodes.UNAUTHORIZED, "未登录");
    }
    return id;
  }

  public static void clear() {
    USER_ID.remove();
    STORE_ID.remove();
  }
}
