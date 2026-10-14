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
 *   <li>cancel — 顾客取消 → 店员
 *   <li>delivering — 开始配送 → 顾客
 * </ul>
 * 发送字段约定：thing1=摘要，thing2=提示语。到货提醒多一个门店名：
 * thing1=商品名，thing2=门店名，thing3=提示语。关键词顺序须与此一致。
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

  public void sendRestock(String openid, String productName, String storeName) {
    Map<String, String> fields = new LinkedHashMap<>();
    fields.put("thing1", truncate(productName, 20));
    fields.put("thing2", truncate(storeLabel(storeName), 20));
    fields.put("thing3", "已到货，可继续预约");
    send(
        "restock",
        appProperties.getSubscribeRestockTemplateId(),
        openid,
        "packageCustomer/pages/home/home",
        fields);
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

  public void sendDelivering(String openid, String summary, String page) {
    send(
        "delivering",
        appProperties.getSubscribeDeliveringTemplateId(),
        openid,
        page,
        truncate(summary, 20),
        "正在配送，请留意收货");
  }

  private void send(
      String kind, String templateId, String openid, String page, String thing1, String thing2) {
    Map<String, String> fields = new LinkedHashMap<>();
    fields.put("thing1", thing1);
    fields.put("thing2", truncate(thing2, 20));
    send(kind, templateId, openid, page, fields);
  }

  private void send(
      String kind, String templateId, String openid, String page, Map<String, String> fields) {
    if (!StringUtils.hasText(openid)) {
      return;
    }
    if (!StringUtils.hasText(templateId)) {
      log.info(
          "subscribe {} skipped (no template): openid={} thing1={}",
          kind,
          openid,
          fields.get("thing1"));
      return;
    }
    try {
      String accessToken = accessTokenService.requireAccessToken();
      Map<String, Object> data = new LinkedHashMap<>();
      fields.forEach((key, value) -> data.put(key, Map.of("value", value == null ? "" : value)));
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

  private static String storeLabel(String storeName) {
    return StringUtils.hasText(storeName) ? storeName.trim() : "本店";
  }

  private static String truncate(String value, int max) {
    if (value == null) {
      return "";
    }
    return value.length() <= max ? value : value.substring(0, max);
  }
}
