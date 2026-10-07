package com.communityfreshmarket.cart.dto;

import java.util.List;

public class CartResponse {

  private List<CartLineResponse> items;
  private int totalFen;
  private int availableCount;
  private int itemCount;

  public CartResponse(List<CartLineResponse> items, int totalFen, int availableCount, int itemCount) {
    this.items = items;
    this.totalFen = totalFen;
    this.availableCount = availableCount;
    this.itemCount = itemCount;
  }

  public List<CartLineResponse> getItems() {
    return items;
  }

  public int getTotalFen() {
    return totalFen;
  }

  public int getAvailableCount() {
    return availableCount;
  }

  public int getItemCount() {
    return itemCount;
  }
}
