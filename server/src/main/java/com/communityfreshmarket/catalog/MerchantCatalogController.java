package com.communityfreshmarket.catalog;

import com.communityfreshmarket.catalog.dto.BooleanFlagRequest;
import com.communityfreshmarket.catalog.dto.CategoryResponse;
import com.communityfreshmarket.catalog.dto.CategorySaveRequest;
import com.communityfreshmarket.catalog.dto.ProductResponse;
import com.communityfreshmarket.catalog.dto.ProductSaveRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/merchant")
public class MerchantCatalogController {

  private final CategoryService categoryService;
  private final ProductService productService;

  public MerchantCatalogController(CategoryService categoryService, ProductService productService) {
    this.categoryService = categoryService;
    this.productService = productService;
  }

  @GetMapping("/categories")
  public ApiResponse<List<CategoryResponse>> categories() {
    return ApiResponse.ok(categoryService.listForMerchant());
  }

  @PostMapping("/categories")
  public ApiResponse<CategoryResponse> createCategory(@Valid @RequestBody CategorySaveRequest request) {
    return ApiResponse.ok(categoryService.create(request));
  }

  @PutMapping("/categories/{id}")
  public ApiResponse<CategoryResponse> updateCategory(
      @PathVariable long id, @Valid @RequestBody CategorySaveRequest request) {
    return ApiResponse.ok(categoryService.update(id, request));
  }

  @DeleteMapping("/categories/{id}")
  public ApiResponse<Void> deleteCategory(@PathVariable long id) {
    categoryService.delete(id);
    return ApiResponse.ok(null);
  }

  @GetMapping("/products")
  public ApiResponse<List<ProductResponse>> products(
      @RequestParam(required = false) Long categoryId) {
    return ApiResponse.ok(productService.listForMerchant(categoryId));
  }

  @GetMapping("/products/{id}")
  public ApiResponse<ProductResponse> product(@PathVariable long id) {
    return ApiResponse.ok(productService.detailForMerchant(id));
  }

  @PostMapping("/products")
  public ApiResponse<ProductResponse> createProduct(@Valid @RequestBody ProductSaveRequest request) {
    return ApiResponse.ok(productService.create(request));
  }

  @PutMapping("/products/{id}")
  public ApiResponse<ProductResponse> updateProduct(
      @PathVariable long id, @Valid @RequestBody ProductSaveRequest request) {
    return ApiResponse.ok(productService.update(id, request));
  }

  @PutMapping("/products/{id}/on-shelf")
  public ApiResponse<ProductResponse> setOnShelf(
      @PathVariable long id, @Valid @RequestBody BooleanFlagRequest request) {
    return ApiResponse.ok(productService.setOnShelf(id, request.getValue()));
  }

  @PutMapping("/products/{id}/in-stock")
  public ApiResponse<ProductResponse> setInStock(
      @PathVariable long id, @Valid @RequestBody BooleanFlagRequest request) {
    return ApiResponse.ok(productService.setInStock(id, request.getValue()));
  }

  @PutMapping("/products/{id}/home-recommended")
  public ApiResponse<ProductResponse> setHomeRecommended(
      @PathVariable long id, @Valid @RequestBody BooleanFlagRequest request) {
    return ApiResponse.ok(productService.setHomeRecommended(id, request.getValue()));
  }
}
