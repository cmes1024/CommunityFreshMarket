package com.communityfreshmarket.catalog;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.communityfreshmarket.common.BizException;
import com.communityfreshmarket.common.ErrorCodes;
import com.communityfreshmarket.domain.Product;
import com.communityfreshmarket.mapper.ProductMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockInventoryService {

  private final ProductMapper productMapper;

  public StockInventoryService(ProductMapper productMapper) {
    this.productMapper = productMapper;
  }

  /** 有库存字段则原子扣减；NULL 表示不追踪数量。 */
  @Transactional
  public void deduct(long productId, long storeId, BigDecimal qty) {
    Product product = productMapper.selectById(productId);
    if (product == null || !product.getStoreId().equals(storeId)) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "商品不存在");
    }
    if (product.getStockQty() == null) {
      if (!Boolean.TRUE.equals(product.getInStock())) {
        throw new BizException(ErrorCodes.BAD_REQUEST, product.getName() + " 已售罄");
      }
      return;
    }
    int updated = productMapper.update(
        null,
        new LambdaUpdateWrapper<Product>()
            .eq(Product::getId, productId)
            .eq(Product::getStoreId, storeId)
            .isNotNull(Product::getStockQty)
            .ge(Product::getStockQty, qty)
            .setSql("stock_qty = stock_qty - " + qty.toPlainString())
            .set(Product::getUpdatedAt, LocalDateTime.now()));
    if (updated == 0) {
      throw new BizException(ErrorCodes.BAD_REQUEST, product.getName() + " 库存不足");
    }
    Product fresh = productMapper.selectById(productId);
    if (fresh.getStockQty() != null && fresh.getStockQty().compareTo(BigDecimal.ZERO) <= 0) {
      productMapper.update(
          null,
          new LambdaUpdateWrapper<Product>()
              .eq(Product::getId, productId)
              .set(Product::getInStock, false)
              .set(Product::getStockQty, BigDecimal.ZERO)
              .set(Product::getUpdatedAt, LocalDateTime.now()));
    }
  }

  @Transactional
  public void restore(long productId, long storeId, BigDecimal qty) {
    Product product = productMapper.selectById(productId);
    if (product == null || !product.getStoreId().equals(storeId)) {
      return;
    }
    if (product.getStockQty() == null) {
      return;
    }
    productMapper.update(
        null,
        new LambdaUpdateWrapper<Product>()
            .eq(Product::getId, productId)
            .eq(Product::getStoreId, storeId)
            .isNotNull(Product::getStockQty)
            .setSql("stock_qty = stock_qty + " + qty.toPlainString())
            .set(Product::getInStock, true)
            .set(Product::getUpdatedAt, LocalDateTime.now()));
  }
}
