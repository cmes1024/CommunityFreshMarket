package com.communityfreshmarket.coupon;

import com.communityfreshmarket.common.ApiResponse;
import com.communityfreshmarket.coupon.dto.CouponClaimResponse;
import com.communityfreshmarket.coupon.dto.CouponResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/coupons")
public class CouponController {

  private final CouponService couponService;

  public CouponController(CouponService couponService) {
    this.couponService = couponService;
  }

  @GetMapping
  public ApiResponse<List<CouponResponse>> list() {
    return ApiResponse.ok(couponService.listEnabled());
  }

  @GetMapping("/mine")
  public ApiResponse<List<CouponClaimResponse>> mine() {
    return ApiResponse.ok(couponService.myClaims());
  }

  @PostMapping("/{id}/claim")
  public ApiResponse<CouponClaimResponse> claim(@PathVariable long id) {
    return ApiResponse.ok(couponService.claim(id));
  }
}
