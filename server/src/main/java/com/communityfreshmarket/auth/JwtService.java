package com.communityfreshmarket.auth;

import com.communityfreshmarket.common.BizException;
import com.communityfreshmarket.common.ErrorCodes;
import com.communityfreshmarket.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final AppProperties appProperties;
  private final SecretKey key;

  public JwtService(AppProperties appProperties) {
    this.appProperties = appProperties;
    byte[] bytes = appProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
    if (bytes.length < 32) {
      throw new IllegalStateException("JWT_SECRET must be at least 32 bytes");
    }
    this.key = Keys.hmacShaKeyFor(bytes);
  }

  public String issue(long userId) {
    Instant now = Instant.now();
    Instant exp = now.plusSeconds(appProperties.getJwtExpireSeconds());
    return Jwts.builder()
        .subject(String.valueOf(userId))
        .issuedAt(Date.from(now))
        .expiration(Date.from(exp))
        .signWith(key)
        .compact();
  }

  public long parseUserId(String token) {
    try {
      Claims claims = Jwts.parser()
          .verifyWith(key)
          .build()
          .parseSignedClaims(token)
          .getPayload();
      return Long.parseLong(claims.getSubject());
    } catch (ExpiredJwtException ex) {
      throw new BizException(ErrorCodes.TOKEN_EXPIRED, "登录已过期");
    } catch (Exception ex) {
      throw new BizException(ErrorCodes.UNAUTHORIZED, "无效登录态");
    }
  }

  public long expireSeconds() {
    return appProperties.getJwtExpireSeconds();
  }
}
