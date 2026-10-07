package com.communityfreshmarket.auth;

import com.communityfreshmarket.auth.dto.LoginResponse;
import com.communityfreshmarket.auth.dto.UserProfileResponse;
import com.communityfreshmarket.auth.dto.WechatLoginRequest;
import com.communityfreshmarket.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/auth/wechat")
  public ApiResponse<LoginResponse> wechatLogin(@Valid @RequestBody WechatLoginRequest request) {
    return ApiResponse.ok(authService.loginByWechat(request));
  }

  @GetMapping("/me")
  public ApiResponse<UserProfileResponse> me() {
    return ApiResponse.ok(authService.currentProfile());
  }
}
