package com.communityfreshmarket.notify;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.communityfreshmarket.auth.UserContext;
import com.communityfreshmarket.common.BizException;
import com.communityfreshmarket.common.ErrorCodes;
import com.communityfreshmarket.domain.Product;
import com.communityfreshmarket.domain.RestockWatch;
import com.communityfreshmarket.domain.User;
import com.communityfreshmarket.mapper.ProductMapper;
import com.communityfreshmarket.mapper.RestockWatchMapper;
import com.communityfreshmarket.mapper.UserMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RestockWatchService {

  private static final Logger log = LoggerFactory.getLogger(RestockWatchService.class);

  private final RestockWatchMapper restockWatchMapper;
  private final ProductMapper productMapper;
  private final UserMapper userMapper;
  private final SubscribeMessageService subscribeMessageService;

  public RestockWatchService(
      RestockWatchMapper restockWatchMapper,
      ProductMapper productMapper,
      UserMapper userMapper,
      SubscribeMessageService subscribeMessageService) {
    this.restockWatchMapper = restockWatchMapper;
    this.productMapper = productMapper;
    this.userMapper = userMapper;
    this.subscribeMessageService = subscribeMessageService;
  }

  @Transactional
  public Map<String, Object> watch(long productId) {
    long userId = UserContext.requireUserId();
    long storeId = UserContext.storeId();
    Product product = productMapper.selectById(productId);
    if (product == null || !product.getStoreId().equals(storeId) || !Boolean.TRUE.equals(product.getOnShelf())) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "商品不可用");
    }
    if (Boolean.TRUE.equals(product.getInStock())) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "商品已有货，可直接加购");
    }

    RestockWatch existing = restockWatchMapper.selectOne(new LambdaQueryWrapper<RestockWatch>()
        .eq(RestockWatch::getUserId, userId)
        .eq(RestockWatch::getStoreId, storeId)
        .eq(RestockWatch::getProductId, productId));
    LocalDateTime now = LocalDateTime.now();
    if (existing == null) {
      RestockWatch watch = new RestockWatch();
      watch.setUserId(userId);
      watch.setStoreId(storeId);
      watch.setProductId(productId);
      watch.setStatus("WATCHING");
      watch.setCreatedAt(now);
      restockWatchMapper.insert(watch);
    } else if (!"WATCHING".equals(existing.getStatus())) {
      existing.setStatus("WATCHING");
      existing.setNotifiedAt(null);
      existing.setCreatedAt(now);
      restockWatchMapper.updateById(existing);
    }
    return Map.of("watching", true, "productId", productId);
  }

  public Map<String, Object> status(long productId) {
    long userId = UserContext.requireUserId();
    long storeId = UserContext.storeId();
    RestockWatch existing = restockWatchMapper.selectOne(new LambdaQueryWrapper<RestockWatch>()
        .eq(RestockWatch::getUserId, userId)
        .eq(RestockWatch::getStoreId, storeId)
        .eq(RestockWatch::getProductId, productId)
        .eq(RestockWatch::getStatus, "WATCHING"));
    return Map.of("watching", existing != null, "productId", productId);
  }

  @Transactional
  public void cancel(long productId) {
    long userId = UserContext.requireUserId();
    long storeId = UserContext.storeId();
    RestockWatch existing = restockWatchMapper.selectOne(new LambdaQueryWrapper<RestockWatch>()
        .eq(RestockWatch::getUserId, userId)
        .eq(RestockWatch::getStoreId, storeId)
        .eq(RestockWatch::getProductId, productId)
        .eq(RestockWatch::getStatus, "WATCHING"));
    if (existing != null) {
      existing.setStatus("CANCELLED");
      restockWatchMapper.updateById(existing);
    }
  }

  /** 商品改回有货时调用；失败不抛出。 */
  public void notifyProductRestocked(Product product) {
    try {
      List<RestockWatch> watchers = restockWatchMapper.selectList(new LambdaQueryWrapper<RestockWatch>()
          .eq(RestockWatch::getStoreId, product.getStoreId())
          .eq(RestockWatch::getProductId, product.getId())
          .eq(RestockWatch::getStatus, "WATCHING"));
      LocalDateTime now = LocalDateTime.now();
      for (RestockWatch watch : watchers) {
        User user = userMapper.selectById(watch.getUserId());
        if (user != null && user.getOpenid() != null) {
          subscribeMessageService.sendRestock(user.getOpenid(), product.getName());
        }
        watch.setStatus("NOTIFIED");
        watch.setNotifiedAt(now);
        restockWatchMapper.updateById(watch);
      }
      log.info("restock notified count={} productId={}", watchers.size(), product.getId());
    } catch (Exception ex) {
      log.warn("restock notify failed productId={}: {}", product.getId(), ex.getMessage());
    }
  }
}
