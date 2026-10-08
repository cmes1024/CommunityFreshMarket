package com.communityfreshmarket.points.dto;

public class MemberCodeResponse {

  private String code;
  private String qrcodeBase64;
  private String nickname;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getQrcodeBase64() {
    return qrcodeBase64;
  }

  public void setQrcodeBase64(String qrcodeBase64) {
    this.qrcodeBase64 = qrcodeBase64;
  }

  public String getNickname() {
    return nickname;
  }

  public void setNickname(String nickname) {
    this.nickname = nickname;
  }
}
