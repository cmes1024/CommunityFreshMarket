package com.communityfreshmarket.cart.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class CartItemRequest {

  @NotNull
  private Long productId;

  @NotNull
  @DecimalMin(value = "0.01", inclusive = true, message = "数量须大于 0")
  private BigDecimal qty;

  public Long getProductId() {
    return productId;
  }

  public void setProductId(Long productId) {
    this.productId = productId;
  }

  public BigDecimal getQty() {
    return qty;
  }

  public void setQty(BigDecimal qty) {
    this.qty = qty;
  }
}
