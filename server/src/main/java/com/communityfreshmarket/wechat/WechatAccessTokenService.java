package com.communityfreshmarket.wechat;

import com.communityfreshmarket.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class WechatAccessTokenService {

  private final AppProperties appProperties;
  private final ObjectMapper objectMapper;
  private final RestClient restClient;
  private final AtomicReference<CachedToken> tokenCache = new AtomicReference<>();

  public WechatAccessTokenService(AppProperties appProperties, ObjectMapper objectMapper) {
    this.appProperties = appProperties;
    this.objectMapper = objectMapper;
    this.restClient = WechatHttp.client();
  }

  public String requireAccessToken() throws Exception {
    CachedToken cached = tokenCache.get();
    long now = System.currentTimeMillis();
    if (cached != null && cached.expireAtMs > now + 60_000) {
      return cached.token;
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
    tokenCache.set(new CachedToken(token, now + expiresIn * 1000L));
    return token;
  }

  private record CachedToken(String token, long expireAtMs) {}
}
