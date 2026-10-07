package com.communityfreshmarket.catalog.dto;

import jakarta.validation.constraints.NotNull;

public class BooleanFlagRequest {

  @NotNull
  private Boolean value;

  public Boolean getValue() {
    return value;
  }

  public void setValue(Boolean value) {
    this.value = value;
  }
}
