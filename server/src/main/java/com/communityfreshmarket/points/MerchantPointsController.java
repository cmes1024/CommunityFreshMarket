package com.communityfreshmarket.points;

import com.communityfreshmarket.common.ApiResponse;
import com.communityfreshmarket.points.dto.CounterPointRequest;
import com.communityfreshmarket.points.dto.CounterPointResponse;
import com.communityfreshmarket.points.dto.PointRedeemItemResponse;
import com.communityfreshmarket.points.dto.PointRedeemItemSaveRequest;
import com.communityfreshmarket.points.dto.PointRedeemResponse;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/merchant/points")
public class MerchantPointsController {

  private final PointsService pointsService;

  public MerchantPointsController(PointsService pointsService) {
    this.pointsService = pointsService;
  }

  @GetMapping("/items")
  public ApiResponse<List<PointRedeemItemResponse>> items() {
    return ApiResponse.ok(pointsService.merchantItems());
  }

  @PostMapping("/items")
  public ApiResponse<PointRedeemItemResponse> createItem(@RequestBody PointRedeemItemSaveRequest request) {
    return ApiResponse.ok(pointsService.createItem(request));
  }

  @PutMapping("/items/{id}")
  public ApiResponse<PointRedeemItemResponse> updateItem(
      @PathVariable long id, @RequestBody PointRedeemItemSaveRequest request) {
    return ApiResponse.ok(pointsService.updateItem(id, request));
  }

  @DeleteMapping("/items/{id}")
  public ApiResponse<Void> deleteItem(@PathVariable long id) {
    pointsService.deleteItem(id);
    return ApiResponse.ok(null);
  }

  @GetMapping("/redeems")
  public ApiResponse<List<PointRedeemResponse>> pending() {
    return ApiResponse.ok(pointsService.merchantPendingRedeems());
  }

  @PostMapping("/redeems/{id}/complete")
  public ApiResponse<PointRedeemResponse> complete(@PathVariable long id) {
    return ApiResponse.ok(pointsService.completeRedeem(id));
  }

  @PostMapping("/redeems/{id}/cancel")
  public ApiResponse<PointRedeemResponse> cancel(@PathVariable long id) {
    return ApiResponse.ok(pointsService.cancelRedeem(id));
  }

  @PostMapping("/counter")
  public ApiResponse<CounterPointResponse> counter(@RequestBody CounterPointRequest request) {
    return ApiResponse.ok(pointsService.awardCounter(request));
  }
}
