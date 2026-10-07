package com.communityfreshmarket.cart;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.communityfreshmarket.auth.UserContext;
import com.communityfreshmarket.cart.dto.CartLineResponse;
import com.communityfreshmarket.cart.dto.CartResponse;
import com.communityfreshmarket.catalog.ProductUnit;
import com.communityfreshmarket.common.BizException;
import com.communityfreshmarket.common.ErrorCodes;
import com.communityfreshmarket.domain.CartItem;
import com.communityfreshmarket.domain.Product;
import com.communityfreshmarket.mapper.CartItemMapper;
import com.communityfreshmarket.mapper.ProductMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

  private static final BigDecimal MAX_QTY = new BigDecimal("999");

  private final CartItemMapper cartItemMapper;
  private final ProductMapper productMapper;

  public CartService(CartItemMapper cartItemMapper, ProductMapper productMapper) {
    this.cartItemMapper = cartItemMapper;
    this.productMapper = productMapper;
  }

  public CartResponse getCart() {
    long userId = UserContext.requireUserId();
    long storeId = UserContext.storeId();
    List<CartItem> items = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
        .eq(CartItem::getUserId, userId)
        .eq(CartItem::getStoreId, storeId)
        .orderByDesc(CartItem::getUpdatedAt));

    Map<Long, Product> products = loadProducts(items);
    List<CartLineResponse> lines = new ArrayList<>();
    int totalFen = 0;
    int availableCount = 0;
    int itemCount = 0;

    for (CartItem item : items) {
      CartLineResponse line = toLine(item, products.get(item.getProductId()));
      lines.add(line);
      itemCount += 1;
      if (line.isAvailable()) {
        availableCount += 1;
        totalFen += line.getAmountFen();
      }
    }
    return new CartResponse(lines, totalFen, availableCount, itemCount);
  }

  @Transactional
  public CartResponse add(long productId, BigDecimal qtyDelta) {
    Product product = requirePurchasable(productId);
    normalizeQty(product.getUnit(), qtyDelta);

    long userId = UserContext.requireUserId();
    long storeId = UserContext.storeId();
    CartItem existing = findItem(userId, storeId, productId);
    LocalDateTime now = LocalDateTime.now();
    if (existing == null) {
      CartItem item = new CartItem();
      item.setUserId(userId);
      item.setStoreId(storeId);
      item.setProductId(productId);
      item.setQty(qtyDelta);
      item.setCreatedAt(now);
      item.setUpdatedAt(now);
      cartItemMapper.insert(item);
    } else {
      BigDecimal next = existing.getQty().add(qtyDelta);
      validateQty(product.getUnit(), next);
      existing.setQty(next);
      existing.setUpdatedAt(now);
      cartItemMapper.updateById(existing);
    }
    return getCart();
  }

  @Transactional
  public CartResponse setQty(long productId, BigDecimal qty) {
    long userId = UserContext.requireUserId();
    long storeId = UserContext.storeId();
    if (qty.compareTo(BigDecimal.ZERO) == 0) {
      deleteItem(userId, storeId, productId);
      return getCart();
    }

    Product product = requirePurchasable(productId);
    validateQty(product.getUnit(), qty);

    CartItem existing = findItem(userId, storeId, productId);
    LocalDateTime now = LocalDateTime.now();
    if (existing == null) {
      CartItem item = new CartItem();
      item.setUserId(userId);
      item.setStoreId(storeId);
      item.setProductId(productId);
      item.setQty(qty);
      item.setCreatedAt(now);
      item.setUpdatedAt(now);
      cartItemMapper.insert(item);
    } else {
      existing.setQty(qty);
      existing.setUpdatedAt(now);
      cartItemMapper.updateById(existing);
    }
    return getCart();
  }

  @Transactional
  public CartResponse remove(long productId) {
    long userId = UserContext.requireUserId();
    long storeId = UserContext.storeId();
    deleteItem(userId, storeId, productId);
    return getCart();
  }

  @Transactional
  public CartResponse clear() {
    long userId = UserContext.requireUserId();
    long storeId = UserContext.storeId();
    cartItemMapper.delete(new LambdaQueryWrapper<CartItem>()
        .eq(CartItem::getUserId, userId)
        .eq(CartItem::getStoreId, storeId));
    return getCart();
  }

  @Transactional
  public void removeProducts(List<Long> productIds) {
    if (productIds == null || productIds.isEmpty()) {
      return;
    }
    long userId = UserContext.requireUserId();
    long storeId = UserContext.storeId();
    cartItemMapper.delete(new LambdaQueryWrapper<CartItem>()
        .eq(CartItem::getUserId, userId)
        .eq(CartItem::getStoreId, storeId)
        .in(CartItem::getProductId, productIds));
  }

  /** 再来一单：可买则累加，不可买返回原因。 */
  @Transactional
  public String tryAdd(long productId, BigDecimal qty) {
    try {
      add(productId, qty);
      return null;
    } catch (BizException ex) {
      return ex.getMessage();
    }
  }

  private Product requirePurchasable(long productId) {
    long storeId = UserContext.storeId();
    Product product = productMapper.selectById(productId);
    if (product == null || !product.getStoreId().equals(storeId) || !Boolean.TRUE.equals(product.getOnShelf())) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "商品不可用");
    }
    if (!Boolean.TRUE.equals(product.getInStock())) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "商品已售罄");
    }
    return product;
  }

  private CartItem findItem(long userId, long storeId, long productId) {
    return cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
        .eq(CartItem::getUserId, userId)
        .eq(CartItem::getStoreId, storeId)
        .eq(CartItem::getProductId, productId));
  }

  private void deleteItem(long userId, long storeId, long productId) {
    cartItemMapper.delete(new LambdaQueryWrapper<CartItem>()
        .eq(CartItem::getUserId, userId)
        .eq(CartItem::getStoreId, storeId)
        .eq(CartItem::getProductId, productId));
  }

  private Map<Long, Product> loadProducts(List<CartItem> items) {
    List<Long> ids = items.stream().map(CartItem::getProductId).distinct().toList();
    if (ids.isEmpty()) {
      return Map.of();
    }
    return productMapper.selectBatchIds(ids).stream()
        .collect(Collectors.toMap(Product::getId, Function.identity()));
  }

  private CartLineResponse toLine(CartItem item, Product product) {
    CartLineResponse line = new CartLineResponse();
    line.setProductId(item.getProductId());
    line.setQty(item.getQty());

    if (product == null || !Objects.equals(product.getStoreId(), item.getStoreId())) {
      line.setName("已失效商品");
      line.setAvailable(false);
      line.setUnavailableReason("商品不存在");
      line.setAmountFen(0);
      return line;
    }

    line.setName(product.getName());
    line.setImageUrl(product.getImageUrl());
    line.setUnit(product.getUnit());
    try {
      line.setUnitLabel(ProductUnit.valueOf(product.getUnit()).getLabel());
    } catch (Exception ex) {
      line.setUnitLabel(product.getUnit());
    }
    line.setPriceFen(product.getPriceFen() == null ? 0 : product.getPriceFen());

    if (!Boolean.TRUE.equals(product.getOnShelf())) {
      line.setAvailable(false);
      line.setUnavailableReason("已下架");
      line.setAmountFen(0);
      return line;
    }
    if (!Boolean.TRUE.equals(product.getInStock())) {
      line.setAvailable(false);
      line.setUnavailableReason("已售罄");
      line.setAmountFen(0);
      return line;
    }

    line.setAvailable(true);
    line.setAmountFen(calcAmountFen(line.getPriceFen(), item.getQty()));
    return line;
  }

  static int calcAmountFen(int priceFen, BigDecimal qty) {
    return BigDecimal.valueOf(priceFen)
        .multiply(qty)
        .setScale(0, RoundingMode.HALF_UP)
        .intValueExact();
  }

  private static void normalizeQty(String unit, BigDecimal qty) {
    validateQty(unit, qty);
  }

  private static void validateQty(String unit, BigDecimal qty) {
    if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "数量须大于 0");
    }
    if (qty.compareTo(MAX_QTY) > 0) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "数量过大");
    }
    ProductUnit u = ProductUnit.require(unit);
    if (u == ProductUnit.JIN) {
      if (qty.scale() > 2) {
        throw new BizException(ErrorCodes.BAD_REQUEST, "斤最多两位小数");
      }
    } else if (qty.stripTrailingZeros().scale() > 0) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "件/份须为整数");
    }
  }
}
