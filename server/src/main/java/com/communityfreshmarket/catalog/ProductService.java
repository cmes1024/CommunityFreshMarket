package com.communityfreshmarket.catalog;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.communityfreshmarket.auth.UserContext;
import com.communityfreshmarket.cart.CartService;
import com.communityfreshmarket.catalog.dto.ProductResponse;
import com.communityfreshmarket.catalog.dto.ProductSaveRequest;
import com.communityfreshmarket.common.BizException;
import com.communityfreshmarket.common.ErrorCodes;
import com.communityfreshmarket.domain.Product;
import com.communityfreshmarket.livephoto.LivePhotoService;
import com.communityfreshmarket.mapper.ProductMapper;
import com.communityfreshmarket.notify.RestockWatchService;
import com.communityfreshmarket.upload.UploadCleanup;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProductService {

  private final ProductMapper productMapper;
  private final CategoryService categoryService;
  private final RestockWatchService restockWatchService;
  private final LivePhotoService livePhotoService;
  private final UploadCleanup uploadCleanup;
  private final CartService cartService;

  public ProductService(
      ProductMapper productMapper,
      CategoryService categoryService,
      RestockWatchService restockWatchService,
      LivePhotoService livePhotoService,
      UploadCleanup uploadCleanup,
      CartService cartService) {
    this.productMapper = productMapper;
    this.categoryService = categoryService;
    this.restockWatchService = restockWatchService;
    this.livePhotoService = livePhotoService;
    this.uploadCleanup = uploadCleanup;
    this.cartService = cartService;
  }

  public List<ProductResponse> listForCustomer(Long categoryId, Boolean homeRecommended) {
    long storeId = UserContext.storeId();
    LambdaQueryWrapper<Product> q = new LambdaQueryWrapper<Product>()
        .eq(Product::getStoreId, storeId)
        .isNull(Product::getDeletedAt)
        .eq(Product::getOnShelf, true)
        .orderByDesc(Product::getInStock)
        .orderByDesc(Product::getUpdatedAt)
        .orderByDesc(Product::getId);
    if (categoryId != null) {
      q.eq(Product::getCategoryId, categoryId);
    }
    if (Boolean.TRUE.equals(homeRecommended)) {
      q.eq(Product::getHomeRecommended, true);
    }
    return productMapper.selectList(q).stream().map(p -> {
      ProductResponse r = ProductResponse.from(p);
      if (!r.isLivePhotoEnabled()) {
        r.setLivePhotoUrl(null);
        r.setLivePhotoTakenAt(null);
      }
      return r;
    }).toList();
  }

  public ProductResponse detailForCustomer(long id) {
    Product product = requireOwned(id);
    if (!Boolean.TRUE.equals(product.getOnShelf())) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "商品不可见");
    }
    ProductResponse response = ProductResponse.from(product);
    livePhotoService.attachCustomer(response, product);
    return response;
  }

  public List<ProductResponse> listForMerchant(Long categoryId) {
    long storeId = UserContext.storeId();
    // 稳定排序：按 id，避免改上架/有货/推荐后因 updatedAt 跳动
    LambdaQueryWrapper<Product> q = new LambdaQueryWrapper<Product>()
        .eq(Product::getStoreId, storeId)
        .isNull(Product::getDeletedAt)
        .orderByDesc(Product::getId);
    if (categoryId != null) {
      q.eq(Product::getCategoryId, categoryId);
    }
    return productMapper.selectList(q).stream().map(ProductResponse::from).toList();
  }

  public ProductResponse detailForMerchant(long id) {
    Product product = requireOwned(id);
    ProductResponse response = ProductResponse.from(product);
    livePhotoService.attachMerchant(response, product);
    return response;
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
    applyImages(product, request);
    product.setUnit(unit.name());
    product.setSpecNote(blankToNull(request.getSpecNote()));
    product.setPriceFen(request.getPriceFen());
    product.setInStock(request.getInStock() == null || request.getInStock());
    product.setOnShelf(request.getOnShelf() == null || request.getOnShelf());
    product.setHomeRecommended(Boolean.TRUE.equals(request.getHomeRecommended()));
    applyQtyLimits(product, request);
    if (product.getLivePhotoEnabled() == null) {
      product.setLivePhotoEnabled(false);
    }
    livePhotoService.applySave(product, request);
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
    List<String> previousImages = referencedImages(product);
    product.setCategoryId(request.getCategoryId());
    product.setName(request.getName().trim());
    applyImages(product, request);
    product.setUnit(unit.name());
    product.setSpecNote(blankToNull(request.getSpecNote()));
    product.setPriceFen(request.getPriceFen());
    if (request.getInStock() != null) {
      product.setInStock(request.getInStock());
    }
    if (request.getOnShelf() != null) {
      product.setOnShelf(request.getOnShelf());
    }
    if (request.getHomeRecommended() != null) {
      product.setHomeRecommended(request.getHomeRecommended());
    }
    applyQtyLimits(product, request);
    livePhotoService.applySave(product, request);
    product.setUpdatedAt(LocalDateTime.now());
    productMapper.updateById(product);

    if (wasOut && Boolean.TRUE.equals(product.getInStock())) {
      notifyRestockWatchers(product);
    }
    previousImages.removeAll(referencedImages(product));
    uploadCleanup.release(previousImages);
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

  @Transactional
  public ProductResponse setHomeRecommended(long id, boolean homeRecommended) {
    Product product = requireOwned(id);
    product.setHomeRecommended(homeRecommended);
    product.setUpdatedAt(LocalDateTime.now());
    productMapper.updateById(product);
    return ProductResponse.from(product);
  }

  @Transactional
  public void delete(long id) {
    removeAll(requireOwnedAlive(List.of(id)));
  }

  @Transactional
  public List<ProductResponse> batch(List<Long> ids, String opCode) {
    ProductBatchOp op = ProductBatchOp.require(opCode);
    List<Product> products = requireOwnedAlive(ids);
    if (op == ProductBatchOp.DELETE) {
      removeAll(products);
      return List.of();
    }
    List<ProductResponse> updated = new ArrayList<>();
    for (Product product : products) {
      updated.add(applyOp(product.getId(), op));
    }
    return updated;
  }

  private ProductResponse applyOp(long id, ProductBatchOp op) {
    return switch (op) {
      case ON_SHELF -> setOnShelf(id, true);
      case OFF_SHELF -> setOnShelf(id, false);
      case IN_STOCK -> setInStock(id, true);
      case SOLD_OUT -> setInStock(id, false);
      case RECOMMEND -> setHomeRecommended(id, true);
      case UNRECOMMEND -> setHomeRecommended(id, false);
      case DELETE -> throw new BizException(ErrorCodes.BAD_REQUEST, "不支持的批量操作");
    };
  }

  private void removeAll(List<Product> products) {
    LocalDateTime now = LocalDateTime.now();
    List<String> images = new ArrayList<>();
    for (Product product : products) {
      images.addAll(referencedImages(product));
      product.setDeletedAt(now);
      product.setOnShelf(false);
      product.setHomeRecommended(false);
      product.setImageUrl(null);
      product.setImageUrls(null);
      product.setLivePhotoUrl(null);
      product.setUpdatedAt(now);
      productMapper.updateById(product);
      cartService.purgeStoreProduct(product.getStoreId(), product.getId());
      restockWatchService.cancelProduct(product.getId());
      livePhotoService.clearOpen(product.getId());
    }
    uploadCleanup.release(images);
  }

  private List<Product> requireOwnedAlive(List<Long> ids) {
    long storeId = UserContext.storeId();
    LinkedHashSet<Long> distinct = new LinkedHashSet<>();
    if (ids != null) {
      for (Long id : ids) {
        if (id != null) {
          distinct.add(id);
        }
      }
    }
    if (distinct.isEmpty()) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "请选择商品");
    }
    if (distinct.size() > 100) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "一次最多操作 100 个商品");
    }
    List<Product> found = productMapper.selectList(new LambdaQueryWrapper<Product>()
        .eq(Product::getStoreId, storeId)
        .isNull(Product::getDeletedAt)
        .in(Product::getId, distinct));
    if (found.size() != distinct.size()) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "有商品不存在或已删除");
    }
    return found;
  }

  private void notifyRestockWatchers(Product product) {
    restockWatchService.notifyProductRestocked(product);
  }

  private Product requireOwned(long id) {
    long storeId = UserContext.storeId();
    Product product = productMapper.selectById(id);
    if (product == null || !product.getStoreId().equals(storeId) || product.getDeletedAt() != null) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "商品不存在");
    }
    return product;
  }

  private static List<String> referencedImages(Product product) {
    List<String> urls = new ArrayList<>(ProductImageUrls.resolve(product.getImageUrls(), product.getImageUrl()));
    if (StringUtils.hasText(product.getLivePhotoUrl())) {
      urls.add(product.getLivePhotoUrl().trim());
    }
    return urls;
  }

  private static void applyImages(Product product, ProductSaveRequest request) {
    var urls = ProductImageUrls.fromRequest(request.getImageUrls(), request.getImageUrl());
    product.setImageUrls(ProductImageUrls.toJson(urls));
    product.setImageUrl(ProductImageUrls.cover(urls));
  }

  private static void applyQtyLimits(Product product, ProductSaveRequest request) {
    ProductQtyRules.validateLimitsConfig(
        request.getMinQty(), request.getMaxQty(), request.getStockQty());
    product.setStockQty(ProductQtyRules.normalizeStock(request.getStockQty()));
    product.setMinQty(ProductQtyRules.normalizeStock(request.getMinQty()));
    product.setMaxQty(ProductQtyRules.normalizeStock(request.getMaxQty()));
    if (product.getStockQty() != null
        && product.getStockQty().compareTo(java.math.BigDecimal.ZERO) <= 0) {
      product.setInStock(false);
      product.setStockQty(java.math.BigDecimal.ZERO);
    }
  }

  private static String blankToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }
}
