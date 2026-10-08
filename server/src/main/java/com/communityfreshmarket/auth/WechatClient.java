package com.communityfreshmarket.auth;

import com.communityfreshmarket.common.BizException;
import com.communityfreshmarket.common.ErrorCodes;
import com.communityfreshmarket.config.AppProperties;
import com.communityfreshmarket.wechat.WechatHttp;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class WechatClient {

  private static final Logger log = LoggerFactory.getLogger(WechatClient.class);

  private final AppProperties appProperties;
  private final RestClient restClient;
  private final ObjectMapper objectMapper;

  public WechatClient(AppProperties appProperties, ObjectMapper objectMapper) {
    this.appProperties = appProperties;
    this.objectMapper = objectMapper;
    this.restClient = WechatHttp.client();
  }

  public String codeToOpenid(String code) {
    String appid = appProperties.getWxAppid();
    String secret = appProperties.getWxSecret();
    if (appid == null || appid.isBlank() || secret == null || secret.isBlank()) {
      throw new BizException(ErrorCodes.LOGIN_FAILED, "未配置微信 AppID/Secret，请用测试登录或补齐环境变量");
    }

    String url = "https://api.weixin.qq.com/sns/jscode2session"
        + "?appid=" + encode(appid)
        + "&secret=" + encode(secret)
        + "&js_code=" + encode(code)
        + "&grant_type=authorization_code";

    try {
      String body = restClient.get().uri(URI.create(url)).retrieve().body(String.class);
      JsonNode node = objectMapper.readTree(body);
      if (node.hasNonNull("errcode") && node.get("errcode").asInt() != 0) {
        log.warn("wechat jscode2session failed: {}", body);
        throw new BizException(ErrorCodes.LOGIN_FAILED, "微信登录失败");
      }
      String openid = node.path("openid").asText(null);
      if (openid == null || openid.isBlank()) {
        throw new BizException(ErrorCodes.LOGIN_FAILED, "微信登录失败");
      }
      return openid;
    } catch (BizException ex) {
      throw ex;
    } catch (Exception ex) {
      log.error("wechat jscode2session error", ex);
      throw new BizException(ErrorCodes.LOGIN_FAILED, "微信登录失败");
    }
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
