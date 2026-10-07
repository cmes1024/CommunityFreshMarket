package com.communityfreshmarket.address;

import com.communityfreshmarket.address.dto.AddressResponse;
import com.communityfreshmarket.address.dto.AddressSaveRequest;
import com.communityfreshmarket.common.ApiResponse;
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
@RequestMapping("/api/v1/addresses")
public class AddressController {

  private final AddressService addressService;

  public AddressController(AddressService addressService) {
    this.addressService = addressService;
  }

  @GetMapping
  public ApiResponse<List<AddressResponse>> list() {
    return ApiResponse.ok(addressService.list());
  }

  @GetMapping("/{id}")
  public ApiResponse<AddressResponse> get(@PathVariable long id) {
    return ApiResponse.ok(addressService.get(id));
  }

  @PostMapping
  public ApiResponse<AddressResponse> create(@Valid @RequestBody AddressSaveRequest request) {
    return ApiResponse.ok(addressService.create(request));
  }

  @PutMapping("/{id}")
  public ApiResponse<AddressResponse> update(
      @PathVariable long id, @Valid @RequestBody AddressSaveRequest request) {
    return ApiResponse.ok(addressService.update(id, request));
  }

  @PutMapping("/{id}/default")
  public ApiResponse<AddressResponse> setDefault(@PathVariable long id) {
    return ApiResponse.ok(addressService.setDefault(id));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable long id) {
    addressService.delete(id);
    return ApiResponse.ok(null);
  }
}
