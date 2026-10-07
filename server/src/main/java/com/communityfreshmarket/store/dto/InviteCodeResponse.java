package com.communityfreshmarket.store.dto;

public class InviteCodeResponse {

  private String code;
  /** 普通链接，扫码打开小程序时由你在公众平台配置规则 */
  private String inviteUrl;
  /** PNG base64，无 data: 前缀 */
  private String qrcodeBase64;

  public InviteCodeResponse() {}

  public InviteCodeResponse(String code, String inviteUrl, String qrcodeBase64) {
    this.code = code;
    this.inviteUrl = inviteUrl;
    this.qrcodeBase64 = qrcodeBase64;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getInviteUrl() {
    return inviteUrl;
  }

  public void setInviteUrl(String inviteUrl) {
    this.inviteUrl = inviteUrl;
  }

  public String getQrcodeBase64() {
    return qrcodeBase64;
  }

  public void setQrcodeBase64(String qrcodeBase64) {
    this.qrcodeBase64 = qrcodeBase64;
  }
}
