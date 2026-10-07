package com.communityfreshmarket.meta;

import com.communityfreshmarket.catalog.ProductUnit;
import com.communityfreshmarket.common.ApiResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/meta")
public class MetaController {

  @GetMapping("/units")
  public ApiResponse<List<Map<String, String>>> units() {
    return ApiResponse.ok(Arrays.stream(ProductUnit.values())
        .map(u -> Map.of("code", u.name(), "label", u.getLabel()))
        .toList());
  }
}
