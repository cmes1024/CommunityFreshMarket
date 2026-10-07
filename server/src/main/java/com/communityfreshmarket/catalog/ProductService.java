package com.communityfreshmarket.catalog;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.communityfreshmarket.auth.UserContext;
import com.communityfreshmarket.catalog.dto.ProductResponse;
import com.communityfreshmarket.catalog.dto.ProductSaveRequest;
import com.communityfreshmarket.common.BizException;
import com.communityfreshmarket.common.ErrorCodes;
import com.communityfreshmarket.domain.Product;
import com.communityfreshmarket.mapper.ProductMapper;
import com.communityfreshmarket.notify.RestockWatchService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProductService {

  private final ProductMapper productMapper;
  private final CategoryService categoryService;
  private final RestockWatchService restockWatchService;

  public ProductService(
      ProductMapper productMapper,
      CategoryService categoryService,
      RestockWatchService restockWatchService) {
    this.productMapper = productMapper;
    this.categoryService = categoryService;
    this.restockWatchService = restockWatchService;
  }

  public List<ProductResponse> listForCustomer(Long categoryId) {
    long storeId = UserContext.storeId();
    LambdaQueryWrapper<Product> q = new LambdaQueryWrapper<Product>()
        .eq(Product::getStoreId, storeId)
        .eq(Product::getOnShelf, true)
        .orderByDesc(Product::getUpdatedAt)
        .orderByDesc(Product::getId);
    if (categoryId != null) {
      q.eq(Product::getCategoryId, categoryId);
    }
    return productMapper.selectList(q).stream().map(ProductResponse::from).toList();
  }

  public ProductResponse detailForCustomer(long id) {
    Product product = requireOwned(id);
    if (!Boolean.TRUE.equals(product.getOnShelf())) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "商品不可见");
    }
    return ProductResponse.from(product);
  }

  public List<ProductResponse> listForMerchant(Long categoryId) {
    long storeId = UserContext.storeId();
    LambdaQueryWrapper<Product> q = new LambdaQueryWrapper<Product>()
        .eq(Product::getStoreId, storeId)
        .orderByDesc(Product::getUpdatedAt)
        .orderByDesc(Product::getId);
    if (categoryId != null) {
      q.eq(Product::getCategoryId, categoryId);
    }
    return productMapper.selectList(q).stream().map(ProductResponse::from).toList();
  }

  public ProductResponse detailForMerchant(long id) {
    return ProductResponse.from(requireOwned(id));
  }

  @Transactional
  public ProductResponse create(ProductSaveRequest request) {
    long storeId = UserContext.storeId();
    categoryService.requireOwnedEnabled(request.getCategoryId(), storeId);
    ProductUnit unit = ProductUnit.require(request.getUnit());

    LocalDateTime now = LocalDateTime.now();
    Product product = new Product();
    product.setStoreId(storeId);
    product.setCategoryId(request.getCategoryId());
    product.setName(request.getName().trim());
    product.setImageUrl(blankToNull(request.getImageUrl()));
    product.setUnit(unit.name());
    product.setSpecNote(blankToNull(request.getSpecNote()));
    product.setPriceFen(request.getPriceFen());
    product.setInStock(request.getInStock() == null || request.getInStock());
    product.setOnShelf(request.getOnShelf() == null || request.getOnShelf());
    product.setCreatedAt(now);
    product.setUpdatedAt(now);
    productMapper.insert(product);
    return ProductResponse.from(product);
  }

  @Transactional
  public ProductResponse update(long id, ProductSaveRequest request) {
    Product product = requireOwned(id);
    long storeId = product.getStoreId();
    categoryService.requireOwnedEnabled(request.getCategoryId(), storeId);
    ProductUnit unit = ProductUnit.require(request.getUnit());

    boolean wasOut = !Boolean.TRUE.equals(product.getInStock());
    product.setCategoryId(request.getCategoryId());
    product.setName(request.getName().trim());
    product.setImageUrl(blankToNull(request.getImageUrl()));
    product.setUnit(unit.name());
    product.setSpecNote(blankToNull(request.getSpecNote()));
    product.setPriceFen(request.getPriceFen());
    if (request.getInStock() != null) {
      product.setInStock(request.getInStock());
    }
    if (request.getOnShelf() != null) {
      product.setOnShelf(request.getOnShelf());
    }
    product.setUpdatedAt(LocalDateTime.now());
    productMapper.updateById(product);

    if (wasOut && Boolean.TRUE.equals(product.getInStock())) {
      notifyRestockWatchers(product);
    }
    return ProductResponse.from(product);
  }

  @Transactional
  public ProductResponse setOnShelf(long id, boolean onShelf) {
    Product product = requireOwned(id);
    product.setOnShelf(onShelf);
    product.setUpdatedAt(LocalDateTime.now());
    productMapper.updateById(product);
    return ProductResponse.from(product);
  }

  @Transactional
  public ProductResponse setInStock(long id, boolean inStock) {
    Product product = requireOwned(id);
    boolean wasOut = !Boolean.TRUE.equals(product.getInStock());
    product.setInStock(inStock);
    product.setUpdatedAt(LocalDateTime.now());
    productMapper.updateById(product);
    if (wasOut && inStock) {
      notifyRestockWatchers(product);
    }
    return ProductResponse.from(product);
  }

  private void notifyRestockWatchers(Product product) {
    restockWatchService.notifyProductRestocked(product);
  }

  private Product requireOwned(long id) {
    long storeId = UserContext.storeId();
    Product product = productMapper.selectById(id);
    if (product == null || !product.getStoreId().equals(storeId)) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "商品不存在");
    }
    return product;
  }

  private static String blankToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }
}
