package com.communityfreshmarket.order.dto;

import com.communityfreshmarket.cart.dto.CartResponse;
import java.util.List;

public class ReorderResponse {

  private CartResponse cart;
  private List<String> skipped;

  public ReorderResponse(CartResponse cart, List<String> skipped) {
    this.cart = cart;
    this.skipped = skipped;
  }

  public CartResponse getCart() {
    return cart;
  }

  public List<String> getSkipped() {
    return skipped;
  }
}
