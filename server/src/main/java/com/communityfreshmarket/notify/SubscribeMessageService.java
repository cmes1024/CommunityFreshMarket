package com.communityfreshmarket.notify;

import com.communityfreshmarket.config.AppProperties;
import com.communityfreshmarket.wechat.WechatAccessTokenService;
import com.communityfreshmarket.wechat.WechatHttp;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * 微信订阅消息。模板 ID 未配置时只记日志，不阻断业务。
 *
 * <p>四类模板（环境变量 / 小程序 config 对照见 deploy/.env.example）：
 * <ul>
 *   <li>restock — 到货提醒 → 顾客
 *   <li>new-order — 新预约 → 店员
 *   <li>cancel — 取消 → 对端
 *   <li>arrived — 顾客到店 → 店员
 * </ul>
 * 发送字段约定：thing1=摘要，thing2=提示语（与公众平台模板关键词对齐后可改）。
 */
@Service
public class SubscribeMessageService {

  private static final Logger log = LoggerFactory.getLogger(SubscribeMessageService.class);

  private final AppProperties appProperties;
  private final WechatAccessTokenService accessTokenService;
  private final ObjectMapper objectMapper;
  private final RestClient restClient;

  public SubscribeMessageService(
      AppProperties appProperties,
      WechatAccessTokenService accessTokenService,
      ObjectMapper objectMapper) {
    this.appProperties = appProperties;
    this.accessTokenService = accessTokenService;
    this.objectMapper = objectMapper;
    this.restClient = WechatHttp.client();
  }

  public void sendRestock(String openid, String productName) {
    send(
        "restock",
        appProperties.getSubscribeRestockTemplateId(),
        openid,
        "packageCustomer/pages/home/home",
        truncate(productName, 20),
        "已到货，可继续预约");
  }

  public void sendNewOrder(String openid, String summary, String page) {
    send(
        "new-order",
        appProperties.getSubscribeNewOrderTemplateId(),
        openid,
        page,
        truncate(summary, 20),
        "有新预约，请尽快确认");
  }

  public void sendCancel(String openid, String summary, String page) {
    send(
        "cancel",
        appProperties.getSubscribeCancelTemplateId(),
        openid,
        page,
        truncate(summary, 20),
        "订单已取消");
  }

  public void sendCustomerArrived(String openid, String summary, String page) {
    send(
        "arrived",
        appProperties.getSubscribeArrivedTemplateId(),
        openid,
        page,
        truncate(summary, 20),
        "顾客已到店，请接待");
  }

  private void send(
      String kind, String templateId, String openid, String page, String thing1, String thing2) {
    if (!StringUtils.hasText(openid)) {
      return;
    }
    if (!StringUtils.hasText(templateId)) {
      log.info("subscribe {} skipped (no template): openid={} thing1={}", kind, openid, thing1);
      return;
    }
    try {
      String accessToken = accessTokenService.requireAccessToken();
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("thing1", Map.of("value", thing1));
      data.put("thing2", Map.of("value", truncate(thing2, 20)));
      Map<String, Object> body = new LinkedHashMap<>();
      body.put("touser", openid);
      body.put("template_id", templateId);
      body.put("page", page == null ? "packageCustomer/pages/home/home" : page);
      body.put("data", data);
      String resp = restClient.post()
          .uri(URI.create(
              "https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=" + accessToken))
          .body(body)
          .retrieve()
          .body(String.class);
      JsonNode node = objectMapper.readTree(resp);
      int errcode = node.path("errcode").asInt(0);
      if (errcode != 0) {
        log.warn("subscribe {} send failed: {}", kind, resp);
      }
    } catch (Exception ex) {
      log.warn("subscribe {} send error: {}", kind, ex.getMessage());
    }
  }

  private static String truncate(String value, int max) {
    if (value == null) {
      return "";
    }
    return value.length() <= max ? value : value.substring(0, max);
  }
}
