package com.communityfreshmarket.order;

import com.communityfreshmarket.common.ApiResponse;
import com.communityfreshmarket.order.dto.AdvanceOrderRequest;
import com.communityfreshmarket.order.dto.CancelOrderRequest;
import com.communityfreshmarket.order.dto.MerchantOrderBoardResponse;
import com.communityfreshmarket.order.dto.OrderResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/merchant")
public class MerchantOrderController {

  private final OrderService orderService;

  public MerchantOrderController(OrderService orderService) {
    this.orderService = orderService;
  }

  @GetMapping("/orders")
  public ApiResponse<MerchantOrderBoardResponse> board() {
    return ApiResponse.ok(orderService.merchantBoard());
  }

  @GetMapping("/orders/{id}")
  public ApiResponse<OrderResponse> detail(@PathVariable long id) {
    return ApiResponse.ok(orderService.merchantDetail(id));
  }

  @PostMapping("/orders/{id}/advance")
  public ApiResponse<OrderResponse> advance(
      @PathVariable long id, @Valid @RequestBody AdvanceOrderRequest request) {
    return ApiResponse.ok(orderService.advance(id, request.getExpectedStatus()));
  }

  @PostMapping("/orders/{id}/cancel")
  public ApiResponse<OrderResponse> cancel(
      @PathVariable long id, @RequestBody(required = false) CancelOrderRequest request) {
    return ApiResponse.ok(orderService.cancelMerchant(id, request));
  }
}

