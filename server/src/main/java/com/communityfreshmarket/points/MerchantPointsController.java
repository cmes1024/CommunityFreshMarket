package com.communityfreshmarket.points;

import com.communityfreshmarket.common.ApiResponse;
import com.communityfreshmarket.points.dto.PointRedeemResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/merchant/points")
public class MerchantPointsController {

  private final PointsService pointsService;

  public MerchantPointsController(PointsService pointsService) {
    this.pointsService = pointsService;
  }

  @GetMapping("/redeems")
  public ApiResponse<List<PointRedeemResponse>> pending() {
    return ApiResponse.ok(pointsService.merchantPendingRedeems());
  }

  @PostMapping("/redeems/{id}/complete")
  public ApiResponse<PointRedeemResponse> complete(@PathVariable long id) {
    return ApiResponse.ok(pointsService.completeRedeem(id));
  }
}
