package com.communityfreshmarket.store;

import com.communityfreshmarket.auth.dto.UserProfileResponse;
import com.communityfreshmarket.common.ApiResponse;
import com.communityfreshmarket.store.dto.AcceptInviteRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/invites")
public class InviteController {

  private final StoreInviteService storeInviteService;

  public InviteController(StoreInviteService storeInviteService) {
    this.storeInviteService = storeInviteService;
  }

  @PostMapping("/accept")
  public ApiResponse<UserProfileResponse> accept(@Valid @RequestBody AcceptInviteRequest request) {
    return ApiResponse.ok(storeInviteService.accept(request.getCode()));
  }
}
