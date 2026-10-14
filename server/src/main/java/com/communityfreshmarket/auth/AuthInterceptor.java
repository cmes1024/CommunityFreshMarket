package com.communityfreshmarket.auth;

import com.communityfreshmarket.common.BizException;
import com.communityfreshmarket.common.ErrorCodes;
import com.communityfreshmarket.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

  public static final String HEADER_STORE_ID = "X-Store-Id";

  private final SessionService sessionService;
  private final AppProperties appProperties;

  public AuthInterceptor(SessionService sessionService, AppProperties appProperties) {
    this.sessionService = sessionService;
    this.appProperties = appProperties;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    String auth = request.getHeader("Authorization");
    if (auth == null || !auth.startsWith("Bearer ")) {
      throw new BizException(ErrorCodes.UNAUTHORIZED, "未登录");
    }
    String token = auth.substring("Bearer ".length()).trim();
    if (token.isEmpty()) {
      throw new BizException(ErrorCodes.UNAUTHORIZED, "未登录");
    }
    long userId = sessionService.parseUserId(token);
    long storeId = resolveStoreId(request);
    UserContext.set(userId, storeId);
    return true;
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    UserContext.clear();
  }

  private long resolveStoreId(HttpServletRequest request) {
    String raw = request.getHeader(HEADER_STORE_ID);
    if (raw == null || raw.isBlank()) {
      return appProperties.getDefaultStoreId();
    }
    try {
      return Long.parseLong(raw.trim());
    } catch (NumberFormatException ex) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "X-Store-Id 无效");
    }
  }
}
