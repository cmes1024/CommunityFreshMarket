package com.communityfreshmarket.livephoto;

import com.communityfreshmarket.common.ApiResponse;
import com.communityfreshmarket.livephoto.dto.LivePhotoRequestItemResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/merchant/live-photo-requests")
public class MerchantLivePhotoController {

  private final LivePhotoService livePhotoService;

  public MerchantLivePhotoController(LivePhotoService livePhotoService) {
    this.livePhotoService = livePhotoService;
  }

  @GetMapping
  public ApiResponse<List<LivePhotoRequestItemResponse>> list() {
    return ApiResponse.ok(livePhotoService.listOpen());
  }
}
