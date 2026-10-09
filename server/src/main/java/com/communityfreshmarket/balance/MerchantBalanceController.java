package com.communityfreshmarket.balance;

import com.communityfreshmarket.balance.dto.BalanceTopupRequest;
import com.communityfreshmarket.balance.dto.BalanceTopupResponse;
import com.communityfreshmarket.common.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/merchant/balance")
public class MerchantBalanceController {

  private final BalanceService balanceService;

  public MerchantBalanceController(BalanceService balanceService) {
    this.balanceService = balanceService;
  }

  @PostMapping("/topups")
  public ApiResponse<BalanceTopupResponse> topUp(@RequestBody BalanceTopupRequest request) {
    return ApiResponse.ok(balanceService.topUp(request));
  }
}
