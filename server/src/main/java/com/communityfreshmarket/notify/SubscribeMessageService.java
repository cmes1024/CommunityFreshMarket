package com.communityfreshmarket.notify;

import com.communityfreshmarket.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * 微信订阅消息。模板 ID 未配置时只记日志，不阻断业务。
 */
@Service
public class SubscribeMessageService {

  private static final Logger log = LoggerFactory.getLogger(SubscribeMessageService.class);

  private final AppProperties appProperties;
  private final ObjectMapper objectMapper;
  private final RestClient restClient;
  private final AtomicReference<CachedToken> tokenCache = new AtomicReference<>();

  public SubscribeMessageService(AppProperties appProperties, ObjectMapper objectMapper) {
    this.appProperties = appProperties;
    this.objectMapper = objectMapper;
    this.restClient = RestClient.create();
  }

  public void sendRestock(String openid, String productName) {
    String templateId = appProperties.getSubscribeRestockTemplateId();
    if (templateId == null || templateId.isBlank()) {
      log.info("subscribe restock skipped (no template): openid={} product={}", openid, productName);
      return;
    }
    try {
      String accessToken = accessToken();
      Map<String, Object> body = Map.of(
          "touser", openid,
          "template_id", templateId,
          "page", "packageCustomer/pages/home/home",
          "data", Map.of(
              "thing1", Map.of("value", truncate(productName, 20)),
              "thing2", Map.of("value", "已到货，可继续预约")));
      String resp = restClient.post()
          .uri(URI.create("https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=" + accessToken))
          .body(body)
          .retrieve()
          .body(String.class);
      JsonNode node = objectMapper.readTree(resp);
      int errcode = node.path("errcode").asInt(0);
      if (errcode != 0) {
        log.warn("subscribe send failed: {}", resp);
      }
    } catch (Exception ex) {
      log.warn("subscribe send error: {}", ex.getMessage());
    }
  }

  private String accessToken() throws Exception {
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

  private static String truncate(String value, int max) {
    if (value == null) {
      return "";
    }
    return value.length() <= max ? value : value.substring(0, max);
  }

  private record CachedToken(String token, long expireAtMs) {}
}
