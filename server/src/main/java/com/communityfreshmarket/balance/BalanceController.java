package com.communityfreshmarket.balance;

import com.communityfreshmarket.balance.dto.BalanceMeResponse;
import com.communityfreshmarket.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/balance")
public class BalanceController {

  private final BalanceService balanceService;

  public BalanceController(BalanceService balanceService) {
    this.balanceService = balanceService;
  }

  @GetMapping("/me")
  public ApiResponse<BalanceMeResponse> mine() {
    return ApiResponse.ok(balanceService.mine());
  }
}
