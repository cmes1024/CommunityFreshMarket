package com.communityfreshmarket.checkin;

import com.communityfreshmarket.checkin.dto.CheckinResponse;
import com.communityfreshmarket.common.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/merchant/checkins")
public class MerchantCheckinController {

  private final CheckinService checkinService;

  public MerchantCheckinController(CheckinService checkinService) {
    this.checkinService = checkinService;
  }

  @GetMapping("/active")
  public ApiResponse<List<CheckinResponse>> active() {
    return ApiResponse.ok(checkinService.listActive());
  }
}
