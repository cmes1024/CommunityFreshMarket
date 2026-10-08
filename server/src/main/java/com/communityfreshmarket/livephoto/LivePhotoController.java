package com.communityfreshmarket.livephoto;

import com.communityfreshmarket.common.ApiResponse;
import com.communityfreshmarket.livephoto.dto.LivePhotoApplyResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class LivePhotoController {

  private final LivePhotoService livePhotoService;

  public LivePhotoController(LivePhotoService livePhotoService) {
    this.livePhotoService = livePhotoService;
  }

  @PostMapping("/products/{productId}/live-photo-requests")
  public ApiResponse<LivePhotoApplyResponse> apply(@PathVariable long productId) {
    return ApiResponse.ok(livePhotoService.apply(productId));
  }
}
