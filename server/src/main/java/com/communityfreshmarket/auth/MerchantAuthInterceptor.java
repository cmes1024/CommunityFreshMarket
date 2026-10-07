package com.communityfreshmarket.auth;

import com.communityfreshmarket.common.BizException;
import com.communityfreshmarket.common.ErrorCodes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class MerchantAuthInterceptor implements HandlerInterceptor {

  private final AuthService authService;

  public MerchantAuthInterceptor(AuthService authService) {
    this.authService = authService;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    Long userId = UserContext.userId();
    Long storeId = UserContext.storeId();
    if (userId == null || storeId == null) {
      throw new BizException(ErrorCodes.UNAUTHORIZED, "未登录");
    }
    authService.requireMembership(userId, storeId);
    return true;
  }
}
