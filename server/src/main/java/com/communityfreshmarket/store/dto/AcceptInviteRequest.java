package com.communityfreshmarket.store.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AcceptInviteRequest {

  @NotBlank
  @Size(max = 16)
  private String code;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }
}
