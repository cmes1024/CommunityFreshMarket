package com.communityfreshmarket.order;

import com.communityfreshmarket.common.ApiResponse;
import com.communityfreshmarket.order.dto.CancelOrderRequest;
import com.communityfreshmarket.order.dto.OrderResponse;
import com.communityfreshmarket.order.dto.PickupSlotResponse;
import com.communityfreshmarket.order.dto.ReorderResponse;
import com.communityfreshmarket.order.dto.SubmitOrderRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class OrderController {

  private final OrderService orderService;
  private final PickupSlotService pickupSlotService;

  public OrderController(OrderService orderService, PickupSlotService pickupSlotService) {
    this.orderService = orderService;
    this.pickupSlotService = pickupSlotService;
  }

  @GetMapping("/pickup-slots")
  public ApiResponse<List<PickupSlotResponse>> pickupSlots() {
    return ApiResponse.ok(pickupSlotService.listEnabled());
  }

  @PostMapping("/orders")
  public ApiResponse<OrderResponse> submit(@Valid @RequestBody SubmitOrderRequest request) {
    return ApiResponse.ok(orderService.submit(request));
  }

  @GetMapping("/orders")
  public ApiResponse<List<OrderResponse>> list(@RequestParam(required = false) String status) {
    return ApiResponse.ok(orderService.listMine(status));
  }

  @GetMapping("/orders/{id}")
  public ApiResponse<OrderResponse> detail(@PathVariable long id) {
    return ApiResponse.ok(orderService.detailMine(id));
  }

  @PostMapping("/orders/{id}/cancel")
  public ApiResponse<OrderResponse> cancel(
      @PathVariable long id, @RequestBody(required = false) CancelOrderRequest request) {
    return ApiResponse.ok(orderService.cancelMine(id, request));
  }

  @PostMapping("/orders/{id}/arrive")
  public ApiResponse<OrderResponse> arrive(@PathVariable long id) {
    return ApiResponse.ok(orderService.arrive(id));
  }

  @PostMapping("/orders/{id}/reorder")
  public ApiResponse<ReorderResponse> reorder(@PathVariable long id) {
    return ApiResponse.ok(orderService.reorder(id));
  }
}
