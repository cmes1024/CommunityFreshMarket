package com.communityfreshmarket.cashier;

import com.communityfreshmarket.cashier.dto.CashierAddItemRequest;
import com.communityfreshmarket.cashier.dto.CashierAttachMemberRequest;
import com.communityfreshmarket.cashier.dto.CashierCompleteRequest;
import com.communityfreshmarket.cashier.dto.CashierSaleResponse;
import com.communityfreshmarket.common.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/merchant/cashier")
public class MerchantCashierController {

  private final CashierService cashierService;

  public MerchantCashierController(CashierService cashierService) {
    this.cashierService = cashierService;
  }

  @PostMapping
  public ApiResponse<CashierSaleResponse> open() {
    return ApiResponse.ok(cashierService.open());
  }

  @GetMapping
  public ApiResponse<List<CashierSaleResponse>> listOpen() {
    return ApiResponse.ok(cashierService.listOpen());
  }

  @GetMapping("/{id}")
  public ApiResponse<CashierSaleResponse> detail(@PathVariable long id) {
    return ApiResponse.ok(cashierService.detail(id));
  }

  @PostMapping("/{id}/items")
  public ApiResponse<CashierSaleResponse> addItem(
      @PathVariable long id, @RequestBody CashierAddItemRequest request) {
    return ApiResponse.ok(cashierService.addItem(id, request));
  }

  @PostMapping("/{id}/member")
  public ApiResponse<CashierSaleResponse> attachMember(
      @PathVariable long id, @RequestBody CashierAttachMemberRequest request) {
    return ApiResponse.ok(cashierService.attachMember(
        id,
        request == null ? null : request.getMemberUserId(),
        request == null ? null : request.getCheckinId()));
  }

  @PostMapping("/{id}/complete")
  public ApiResponse<CashierSaleResponse> complete(
      @PathVariable long id, @RequestBody(required = false) CashierCompleteRequest request) {
    return ApiResponse.ok(cashierService.complete(id, request));
  }

  @PostMapping("/{id}/cancel")
  public ApiResponse<CashierSaleResponse> cancel(@PathVariable long id) {
    return ApiResponse.ok(cashierService.cancel(id));
  }
}
