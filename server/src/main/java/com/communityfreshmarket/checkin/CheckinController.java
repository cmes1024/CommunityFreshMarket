package com.communityfreshmarket.checkin;

import com.communityfreshmarket.checkin.dto.CheckinRequest;
import com.communityfreshmarket.checkin.dto.CheckinResponse;
import com.communityfreshmarket.common.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/checkins")
public class CheckinController {

  private final CheckinService checkinService;

  public CheckinController(CheckinService checkinService) {
    this.checkinService = checkinService;
  }

  @PostMapping
  public ApiResponse<CheckinResponse> checkin(@RequestBody(required = false) CheckinRequest request) {
    return ApiResponse.ok(checkinService.checkin(request));
  }
}
