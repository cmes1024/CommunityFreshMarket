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
  private String subscribeRestockTemplateId = "";

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
}
