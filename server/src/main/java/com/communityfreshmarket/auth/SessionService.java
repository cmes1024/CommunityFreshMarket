package com.communityfreshmarket.auth;

import com.communityfreshmarket.common.BizException;
import com.communityfreshmarket.common.ErrorCodes;
import com.communityfreshmarket.config.AppProperties;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/** 登录令牌是随机串，用户编号放在 Redis，有效期从签发起算，不随请求续期。 */
@Service
public class SessionService {

  static final String KEY_PREFIX = "cfm:session:";

  private final AppProperties appProperties;
  private final StringRedisTemplate redis;
  private final SecureRandom random = new SecureRandom();

  public SessionService(AppProperties appProperties, StringRedisTemplate redis) {
    this.appProperties = appProperties;
    this.redis = redis;
  }

  public String issue(long userId) {
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    redis.opsForValue()
        .set(KEY_PREFIX + token, Long.toString(userId), Duration.ofSeconds(expireSeconds()));
    return token;
  }

  public long parseUserId(String token) {
    String userId = redis.opsForValue().get(KEY_PREFIX + token);
    if (userId == null || userId.isBlank()) {
      throw new BizException(ErrorCodes.TOKEN_EXPIRED, "登录已过期");
    }
    try {
      return Long.parseLong(userId);
    } catch (NumberFormatException ex) {
      throw new BizException(ErrorCodes.TOKEN_EXPIRED, "登录已过期");
    }
  }

  public long expireSeconds() {
    return appProperties.getSessionExpireSeconds();
  }
}
