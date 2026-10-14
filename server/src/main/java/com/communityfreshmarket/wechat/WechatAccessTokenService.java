package com.communityfreshmarket.wechat;

import com.communityfreshmarket.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class WechatAccessTokenService {

  static final String KEY = "cfm:wx:access_token";

  private final AppProperties appProperties;
  private final ObjectMapper objectMapper;
  private final StringRedisTemplate redis;
  private final RestClient restClient;

  public WechatAccessTokenService(
      AppProperties appProperties, ObjectMapper objectMapper, StringRedisTemplate redis) {
    this.appProperties = appProperties;
    this.objectMapper = objectMapper;
    this.redis = redis;
    this.restClient = WechatHttp.client();
  }

  public String requireAccessToken() throws Exception {
    String cached = redis.opsForValue().get(KEY);
    if (cached != null && !cached.isBlank()) {
      return cached;
    }
    String appid = appProperties.getWxAppid();
    String secret = appProperties.getWxSecret();
    if (appid == null || appid.isBlank() || secret == null || secret.isBlank()) {
      throw new IllegalStateException("WX_APPID/WX_SECRET not configured");
    }
    String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential"
        + "&appid=" + appid + "&secret=" + secret;
    String resp = restClient.get().uri(URI.create(url)).retrieve().body(String.class);
    JsonNode node = objectMapper.readTree(resp);
    String token = node.path("access_token").asText(null);
    int expiresIn = node.path("expires_in").asInt(7200);
    if (token == null || token.isBlank()) {
      throw new IllegalStateException("cannot get access_token: " + resp);
    }
    long ttl = Math.max(60, expiresIn - 60L);
    redis.opsForValue().set(KEY, token, Duration.ofSeconds(ttl));
    return token;
  }
}
