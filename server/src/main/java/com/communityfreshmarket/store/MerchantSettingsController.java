package com.communityfreshmarket.store;

import com.communityfreshmarket.catalog.StoreSummaryResponse;
import com.communityfreshmarket.common.ApiResponse;
import com.communityfreshmarket.order.PickupSlotService;
import com.communityfreshmarket.order.dto.PickupSlotResponse;
import com.communityfreshmarket.order.dto.PickupSlotSaveRequest;
import com.communityfreshmarket.store.dto.StoreUpdateRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/merchant")
public class MerchantSettingsController {

  private final StoreSettingsService storeSettingsService;
  private final PickupSlotService pickupSlotService;

  public MerchantSettingsController(
      StoreSettingsService storeSettingsService, PickupSlotService pickupSlotService) {
    this.storeSettingsService = storeSettingsService;
    this.pickupSlotService = pickupSlotService;
  }

  @PutMapping("/store")
  public ApiResponse<StoreSummaryResponse> updateStore(@Valid @RequestBody StoreUpdateRequest request) {
    return ApiResponse.ok(storeSettingsService.update(request));
  }

  @GetMapping("/pickup-slots")
  public ApiResponse<List<PickupSlotResponse>> pickupSlots() {
    return ApiResponse.ok(pickupSlotService.listForMerchant());
  }

  @PostMapping("/pickup-slots")
  public ApiResponse<PickupSlotResponse> createSlot(@Valid @RequestBody PickupSlotSaveRequest request) {
    return ApiResponse.ok(pickupSlotService.create(request));
  }

  @PutMapping("/pickup-slots/{id}")
  public ApiResponse<PickupSlotResponse> updateSlot(
      @PathVariable long id, @Valid @RequestBody PickupSlotSaveRequest request) {
    return ApiResponse.ok(pickupSlotService.update(id, request));
  }

  @DeleteMapping("/pickup-slots/{id}")
  public ApiResponse<Void> deleteSlot(@PathVariable long id) {
    pickupSlotService.delete(id);
    return ApiResponse.ok(null);
  }
}
