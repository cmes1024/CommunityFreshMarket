package com.communityfreshmarket.points;

import com.communityfreshmarket.common.ApiResponse;
import com.communityfreshmarket.points.dto.MemberCodeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/member-code")
public class MemberCodeController {

  private final MemberCodeService memberCodeService;

  public MemberCodeController(MemberCodeService memberCodeService) {
    this.memberCodeService = memberCodeService;
  }

  @GetMapping
  public ApiResponse<MemberCodeResponse> mine() {
    return ApiResponse.ok(memberCodeService.mine());
  }
}
