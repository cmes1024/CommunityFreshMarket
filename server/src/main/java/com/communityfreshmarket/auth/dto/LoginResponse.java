package com.communityfreshmarket.auth.dto;

public class LoginResponse {

  private String token;
  private long expiresIn;
  private UserProfileResponse user;

  public LoginResponse(String token, long expiresIn, UserProfileResponse user) {
    this.token = token;
    this.expiresIn = expiresIn;
    this.user = user;
  }

  public String getToken() {
    return token;
  }

  public long getExpiresIn() {
    return expiresIn;
  }

  public UserProfileResponse getUser() {
    return user;
  }
}
