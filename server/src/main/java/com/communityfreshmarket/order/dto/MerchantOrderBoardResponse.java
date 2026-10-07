package com.communityfreshmarket.order.dto;

import java.util.List;

public class MerchantOrderBoardResponse {

  private List<OrderResponse> urgent;
  private List<OrderResponse> others;

  public MerchantOrderBoardResponse(List<OrderResponse> urgent, List<OrderResponse> others) {
    this.urgent = urgent;
    this.others = others;
  }

  public List<OrderResponse> getUrgent() {
    return urgent;
  }

  public List<OrderResponse> getOthers() {
    return others;
  }
}
