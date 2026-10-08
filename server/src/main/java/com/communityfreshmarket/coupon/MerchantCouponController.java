package com.communityfreshmarket.coupon;

import com.communityfreshmarket.common.ApiResponse;
import com.communityfreshmarket.coupon.dto.CouponClaimResponse;
import com.communityfreshmarket.coupon.dto.CouponResponse;
import com.communityfreshmarket.coupon.dto.CouponSaveRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/merchant/coupons")
public class MerchantCouponController {

  private final CouponService couponService;

  public MerchantCouponController(CouponService couponService) {
    this.couponService = couponService;
  }

  @GetMapping
  public ApiResponse<List<CouponResponse>> list() {
    return ApiResponse.ok(couponService.listMerchant());
  }

  @GetMapping("/unused")
  public ApiResponse<List<CouponClaimResponse>> unused(
      @RequestParam long userId, @RequestParam(required = false) Long saleId) {
    return ApiResponse.ok(couponService.usableForCashier(userId, saleId));
  }

  @PostMapping
  public ApiResponse<CouponResponse> create(@Valid @RequestBody CouponSaveRequest request) {
    return ApiResponse.ok(couponService.create(request));
  }

  @PutMapping("/{id}")
  public ApiResponse<CouponResponse> update(
      @PathVariable long id, @Valid @RequestBody CouponSaveRequest request) {
    return ApiResponse.ok(couponService.update(id, request));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable long id) {
    couponService.delete(id);
    return ApiResponse.ok(null);
  }
}
