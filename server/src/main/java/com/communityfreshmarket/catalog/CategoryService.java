package com.communityfreshmarket.catalog;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.communityfreshmarket.auth.UserContext;
import com.communityfreshmarket.catalog.dto.CategoryResponse;
import com.communityfreshmarket.catalog.dto.CategorySaveRequest;
import com.communityfreshmarket.common.BizException;
import com.communityfreshmarket.common.ErrorCodes;
import com.communityfreshmarket.domain.Category;
import com.communityfreshmarket.domain.Product;
import com.communityfreshmarket.mapper.CategoryMapper;
import com.communityfreshmarket.mapper.ProductMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

  private final CategoryMapper categoryMapper;
  private final ProductMapper productMapper;

  public CategoryService(CategoryMapper categoryMapper, ProductMapper productMapper) {
    this.categoryMapper = categoryMapper;
    this.productMapper = productMapper;
  }

  public List<CategoryResponse> listForCustomer() {
    long storeId = UserContext.storeId();
    return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
            .eq(Category::getStoreId, storeId)
            .eq(Category::getEnabled, true)
            .orderByAsc(Category::getSortOrder)
            .orderByAsc(Category::getId))
        .stream()
        .map(CategoryResponse::from)
        .toList();
  }

  public List<CategoryResponse> listForMerchant() {
    long storeId = UserContext.storeId();
    return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
            .eq(Category::getStoreId, storeId)
            .orderByAsc(Category::getSortOrder)
            .orderByAsc(Category::getId))
        .stream()
        .map(CategoryResponse::from)
        .toList();
  }

  @Transactional
  public CategoryResponse create(CategorySaveRequest request) {
    long storeId = UserContext.storeId();
    LocalDateTime now = LocalDateTime.now();
    Category category = new Category();
    category.setStoreId(storeId);
    category.setName(request.getName().trim());
    category.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
    category.setEnabled(request.getEnabled() == null || request.getEnabled());
    category.setCreatedAt(now);
    category.setUpdatedAt(now);
    categoryMapper.insert(category);
    return CategoryResponse.from(category);
  }

  @Transactional
  public CategoryResponse update(long id, CategorySaveRequest request) {
    Category category = requireOwned(id);
    category.setName(request.getName().trim());
    if (request.getSortOrder() != null) {
      category.setSortOrder(request.getSortOrder());
    }
    if (request.getEnabled() != null) {
      category.setEnabled(request.getEnabled());
    }
    category.setUpdatedAt(LocalDateTime.now());
    categoryMapper.updateById(category);
    return CategoryResponse.from(category);
  }

  @Transactional
  public void delete(long id) {
    Category category = requireOwned(id);
    Long productCount = productMapper.selectCount(new LambdaQueryWrapper<Product>()
        .eq(Product::getStoreId, category.getStoreId())
        .eq(Product::getCategoryId, id));
    if (productCount != null && productCount > 0) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "分类下仍有商品，请先停用或移走商品");
    }
    categoryMapper.deleteById(id);
  }

  public Category requireOwnedEnabled(long categoryId, long storeId) {
    Category category = categoryMapper.selectById(categoryId);
    if (category == null || !category.getStoreId().equals(storeId)) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "分类不存在");
    }
    if (!Boolean.TRUE.equals(category.getEnabled())) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "分类未启用");
    }
    return category;
  }

  private Category requireOwned(long id) {
    long storeId = UserContext.storeId();
    Category category = categoryMapper.selectById(id);
    if (category == null || !category.getStoreId().equals(storeId)) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "分类不存在");
    }
    return category;
  }
}
