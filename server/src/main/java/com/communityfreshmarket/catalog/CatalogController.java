package com.communityfreshmarket.catalog;

import com.communityfreshmarket.catalog.dto.CategoryResponse;
import com.communityfreshmarket.catalog.dto.ProductResponse;
import com.communityfreshmarket.common.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CatalogController {

  private final CategoryService categoryService;
  private final ProductService productService;
  private final StoreQueryService storeQueryService;

  public CatalogController(
      CategoryService categoryService,
      ProductService productService,
      StoreQueryService storeQueryService) {
    this.categoryService = categoryService;
    this.productService = productService;
    this.storeQueryService = storeQueryService;
  }

  @GetMapping("/store")
  public ApiResponse<StoreSummaryResponse> store() {
    return ApiResponse.ok(storeQueryService.currentStore());
  }

  @GetMapping("/categories")
  public ApiResponse<List<CategoryResponse>> categories() {
    return ApiResponse.ok(categoryService.listForCustomer());
  }

  @GetMapping("/products")
  public ApiResponse<List<ProductResponse>> products(
      @RequestParam(required = false) Long categoryId) {
    return ApiResponse.ok(productService.listForCustomer(categoryId));
  }

  @GetMapping("/products/{id}")
  public ApiResponse<ProductResponse> product(@PathVariable long id) {
    return ApiResponse.ok(productService.detailForCustomer(id));
  }
}
