package com.communityfreshmarket.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

  private long defaultStoreId = 1L;
  private String jwtSecret = "";
  private long jwtExpireSeconds = 604800L;
  private String wxAppid = "";
  private String wxSecret = "";
  /** 店主冷启动：该微信 openid 登录时自动写入默认店 OWNER（与环境无关） */
  private String bootstrapOwnerOpenid = "";
  private String uploadDir = "/app/uploads";
  private String uploadPublicBase = "/uploads";
  /** 到货提醒：售罄订阅后，改回有货发给顾客 */
  private String subscribeRestockTemplateId = "";
  /** 新预约：顾客下单后发给本店成员 */
  private String subscribeNewOrderTemplateId = "";
  /** 取消：顾客取消→店员，店员取消→顾客 */
  private String subscribeCancelTemplateId = "";
  /** 顾客到店：待自提报到后发给本店成员 */
  private String subscribeArrivedTemplateId = "";
  /**
   * 邀请普通链接前缀（不含 code）。扫码打开小程序时由公众平台配置。
   * 例：https://your.domain/cfm/join → 实际链接 https://your.domain/cfm/join?code=AB12CD
   */
  private String inviteLinkBase = "";

  public long getDefaultStoreId() {
    return defaultStoreId;
  }

  public void setDefaultStoreId(long defaultStoreId) {
    this.defaultStoreId = defaultStoreId;
  }

  public String getJwtSecret() {
    return jwtSecret;
  }

  public void setJwtSecret(String jwtSecret) {
    this.jwtSecret = jwtSecret;
  }

  public long getJwtExpireSeconds() {
    return jwtExpireSeconds;
  }

  public void setJwtExpireSeconds(long jwtExpireSeconds) {
    this.jwtExpireSeconds = jwtExpireSeconds;
  }

  public String getWxAppid() {
    return wxAppid;
  }

  public void setWxAppid(String wxAppid) {
    this.wxAppid = wxAppid;
  }

  public String getWxSecret() {
    return wxSecret;
  }

  public void setWxSecret(String wxSecret) {
    this.wxSecret = wxSecret;
  }

  public String getBootstrapOwnerOpenid() {
    return bootstrapOwnerOpenid;
  }

  public void setBootstrapOwnerOpenid(String bootstrapOwnerOpenid) {
    this.bootstrapOwnerOpenid = bootstrapOwnerOpenid;
  }

  public String getUploadDir() {
    return uploadDir;
  }

  public void setUploadDir(String uploadDir) {
    this.uploadDir = uploadDir;
  }

  public String getUploadPublicBase() {
    return uploadPublicBase;
  }

  public void setUploadPublicBase(String uploadPublicBase) {
    this.uploadPublicBase = uploadPublicBase;
  }

  public String getSubscribeRestockTemplateId() {
    return subscribeRestockTemplateId;
  }

  public void setSubscribeRestockTemplateId(String subscribeRestockTemplateId) {
    this.subscribeRestockTemplateId = subscribeRestockTemplateId;
  }

  public String getSubscribeNewOrderTemplateId() {
    return subscribeNewOrderTemplateId;
  }

  public void setSubscribeNewOrderTemplateId(String subscribeNewOrderTemplateId) {
    this.subscribeNewOrderTemplateId = subscribeNewOrderTemplateId;
  }

  public String getSubscribeCancelTemplateId() {
    return subscribeCancelTemplateId;
  }

  public void setSubscribeCancelTemplateId(String subscribeCancelTemplateId) {
    this.subscribeCancelTemplateId = subscribeCancelTemplateId;
  }

  public String getSubscribeArrivedTemplateId() {
    return subscribeArrivedTemplateId;
  }

  public void setSubscribeArrivedTemplateId(String subscribeArrivedTemplateId) {
    this.subscribeArrivedTemplateId = subscribeArrivedTemplateId;
  }

  public String getInviteLinkBase() {
    return inviteLinkBase;
  }

  public void setInviteLinkBase(String inviteLinkBase) {
    this.inviteLinkBase = inviteLinkBase;
  }
}
