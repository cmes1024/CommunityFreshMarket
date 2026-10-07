package com.communityfreshmarket.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class WechatLoginRequest {

  /** 微信 wx.login 返回的 code */
  @NotBlank
  private String code;

  private String nickname;
  private String avatarUrl;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getNickname() {
    return nickname;
  }

  public void setNickname(String nickname) {
    this.nickname = nickname;
  }

  public String getAvatarUrl() {
    return avatarUrl;
  }

  public void setAvatarUrl(String avatarUrl) {
    this.avatarUrl = avatarUrl;
  }
}
