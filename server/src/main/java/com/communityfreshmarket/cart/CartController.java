package com.communityfreshmarket.cart;

import com.communityfreshmarket.cart.dto.CartItemRequest;
import com.communityfreshmarket.cart.dto.CartQtyRequest;
import com.communityfreshmarket.cart.dto.CartResponse;
import com.communityfreshmarket.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

  private final CartService cartService;

  public CartController(CartService cartService) {
    this.cartService = cartService;
  }

  @GetMapping
  public ApiResponse<CartResponse> get() {
    return ApiResponse.ok(cartService.getCart());
  }

  @PostMapping("/items")
  public ApiResponse<CartResponse> add(@Valid @RequestBody CartItemRequest request) {
    return ApiResponse.ok(cartService.add(request.getProductId(), request.getQty()));
  }

  @PutMapping("/items/{productId}")
  public ApiResponse<CartResponse> setQty(
      @PathVariable long productId, @Valid @RequestBody CartQtyRequest request) {
    return ApiResponse.ok(cartService.setQty(productId, request.getQty()));
  }

  @DeleteMapping("/items/{productId}")
  public ApiResponse<CartResponse> remove(@PathVariable long productId) {
    return ApiResponse.ok(cartService.remove(productId));
  }

  @DeleteMapping
  public ApiResponse<CartResponse> clear() {
    return ApiResponse.ok(cartService.clear());
  }
}
