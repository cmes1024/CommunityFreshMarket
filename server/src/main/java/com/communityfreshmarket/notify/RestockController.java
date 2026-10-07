package com.communityfreshmarket.notify;

import com.communityfreshmarket.common.ApiResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products/{productId}/restock-watch")
public class RestockController {

  private final RestockWatchService restockWatchService;

  public RestockController(RestockWatchService restockWatchService) {
    this.restockWatchService = restockWatchService;
  }

  @GetMapping
  public ApiResponse<Map<String, Object>> status(@PathVariable long productId) {
    return ApiResponse.ok(restockWatchService.status(productId));
  }

  @PostMapping
  public ApiResponse<Map<String, Object>> watch(@PathVariable long productId) {
    return ApiResponse.ok(restockWatchService.watch(productId));
  }

  @DeleteMapping
  public ApiResponse<Void> cancel(@PathVariable long productId) {
    restockWatchService.cancel(productId);
    return ApiResponse.ok(null);
  }
}
