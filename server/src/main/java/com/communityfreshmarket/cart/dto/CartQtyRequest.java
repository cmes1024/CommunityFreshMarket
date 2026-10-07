package com.communityfreshmarket.cart.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class CartQtyRequest {

  @NotNull
  @DecimalMin(value = "0", inclusive = true, message = "数量不能为负")
  private BigDecimal qty;

  public BigDecimal getQty() {
    return qty;
  }

  public void setQty(BigDecimal qty) {
    this.qty = qty;
  }
}
