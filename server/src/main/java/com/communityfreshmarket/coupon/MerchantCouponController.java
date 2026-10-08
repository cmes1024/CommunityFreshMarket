package com.communityfreshmarket.coupon;

import com.communityfreshmarket.common.ApiResponse;
import com.communityfreshmarket.coupon.dto.CouponResponse;
import com.communityfreshmarket.coupon.dto.CouponSaveRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

  @PostMapping
  public ApiResponse<CouponResponse> create(@Valid @RequestBody CouponSaveRequest request) {
    return ApiResponse.ok(couponService.create(request));
  }

  @PutMapping("/{id}")
  public ApiResponse<CouponResponse> update(
      @PathVariable long id, @Valid @RequestBody CouponSaveRequest request) {
    return ApiResponse.ok(couponService.update(id, request));
  }
}
