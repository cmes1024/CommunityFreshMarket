package com.communityfreshmarket.points;

import com.communityfreshmarket.common.ApiResponse;
import com.communityfreshmarket.points.dto.PointRedeemRequest;
import com.communityfreshmarket.points.dto.PointRedeemResponse;
import com.communityfreshmarket.points.dto.PointsMeResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/points")
public class PointsController {

  private final PointsService pointsService;

  public PointsController(PointsService pointsService) {
    this.pointsService = pointsService;
  }

  @GetMapping("/me")
  public ApiResponse<PointsMeResponse> me() {
    return ApiResponse.ok(pointsService.me());
  }

  @PostMapping("/redeem")
  public ApiResponse<PointRedeemResponse> redeem(@Valid @RequestBody PointRedeemRequest request) {
    return ApiResponse.ok(pointsService.redeem(request));
  }
}
