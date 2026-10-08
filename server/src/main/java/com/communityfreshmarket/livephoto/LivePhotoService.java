package com.communityfreshmarket.livephoto;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.communityfreshmarket.auth.UserContext;
import com.communityfreshmarket.catalog.dto.ProductResponse;
import com.communityfreshmarket.catalog.dto.ProductSaveRequest;
import com.communityfreshmarket.common.BizException;
import com.communityfreshmarket.common.ErrorCodes;
import com.communityfreshmarket.domain.LivePhotoRequest;
import com.communityfreshmarket.domain.LivePhotoRequestUser;
import com.communityfreshmarket.domain.Product;
import com.communityfreshmarket.domain.Store;
import com.communityfreshmarket.livephoto.dto.LivePhotoApplyResponse;
import com.communityfreshmarket.livephoto.dto.LivePhotoRequestItemResponse;
import com.communityfreshmarket.mapper.LivePhotoRequestMapper;
import com.communityfreshmarket.mapper.LivePhotoRequestUserMapper;
import com.communityfreshmarket.mapper.ProductMapper;
import com.communityfreshmarket.mapper.StoreMapper;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class LivePhotoService {

  private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

  private final LivePhotoRequestMapper requestMapper;
  private final LivePhotoRequestUserMapper requestUserMapper;
  private final ProductMapper productMapper;
  private final StoreMapper storeMapper;

  public LivePhotoService(
      LivePhotoRequestMapper requestMapper,
      LivePhotoRequestUserMapper requestUserMapper,
      ProductMapper productMapper,
      StoreMapper storeMapper) {
    this.requestMapper = requestMapper;
    this.requestUserMapper = requestUserMapper;
    this.productMapper = productMapper;
    this.storeMapper = storeMapper;
  }

  private static LocalDateTime now() {
    return LocalDateTime.now(ZONE);
  }

  public void applySave(Product product, ProductSaveRequest request) {
    if (request.getLivePhotoEnabled() != null) {
      product.setLivePhotoEnabled(Boolean.TRUE.equals(request.getLivePhotoEnabled()));
    } else if (product.getLivePhotoEnabled() == null) {
      product.setLivePhotoEnabled(false);
    }
    if (request.getLivePhotoUrl() == null) {
      return;
    }
    String url = StringUtils.hasText(request.getLivePhotoUrl()) ? request.getLivePhotoUrl().trim() : null;
    String prev = StringUtils.hasText(product.getLivePhotoUrl()) ? product.getLivePhotoUrl().trim() : null;
    boolean changed = url == null ? prev != null : !url.equals(prev);
    product.setLivePhotoUrl(url);
    if (url == null) {
      product.setLivePhotoTakenAt(null);
      return;
    }
    if (changed) {
      product.setLivePhotoTakenAt(now());
      if (product.getId() != null) {
        clearOpen(product.getId());
      }
    }
  }

  public void attachCustomer(ProductResponse response, Product product) {
    boolean enabled = Boolean.TRUE.equals(product.getLivePhotoEnabled());
    response.setLivePhotoEnabled(enabled);
    if (!enabled) {
      response.setLivePhotoUrl(null);
      response.setLivePhotoTakenAt(null);
      response.setLivePhotoExpired(false);
      response.setLivePhotoApplicantCount(0);
      response.setLivePhotoRequestedByMe(false);
      return;
    }
    response.setLivePhotoUrl(blankToNull(product.getLivePhotoUrl()));
    response.setLivePhotoTakenAt(product.getLivePhotoTakenAt());
    response.setLivePhotoExpired(isExpired(product));
    fillRequest(response, product.getId(), UserContext.userId());
  }

  public void attachMerchant(ProductResponse response, Product product) {
    boolean enabled = Boolean.TRUE.equals(product.getLivePhotoEnabled());
    response.setLivePhotoEnabled(enabled);
    response.setLivePhotoUrl(blankToNull(product.getLivePhotoUrl()));
    response.setLivePhotoTakenAt(product.getLivePhotoTakenAt());
    response.setLivePhotoExpired(enabled && isExpired(product));
    fillRequest(response, product.getId(), null);
  }

  @Transactional
  public LivePhotoApplyResponse apply(long productId) {
    long userId = UserContext.requireUserId();
    long storeId = UserContext.storeId();
    Product product = productMapper.selectById(productId);
    if (product == null
        || !product.getStoreId().equals(storeId)
        || product.getDeletedAt() != null
        || !Boolean.TRUE.equals(product.getOnShelf())) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "商品不存在");
    }
    if (!Boolean.TRUE.equals(product.getLivePhotoEnabled())) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "该商品未开启实景图");
    }
    if (StringUtils.hasText(product.getLivePhotoUrl()) && !isExpired(product)) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "实景图仍在有效期内");
    }
    LivePhotoRequest open = openRequest(productId);
    if (open == null) {
      open = insertOpen(storeId, productId);
    }
    LivePhotoRequestUser mine = requestUserMapper.selectOne(new LambdaQueryWrapper<LivePhotoRequestUser>()
        .eq(LivePhotoRequestUser::getRequestId, open.getId())
        .eq(LivePhotoRequestUser::getUserId, userId)
        .last("LIMIT 1"));
    if (mine != null) {
      return new LivePhotoApplyResponse(countOf(open), true);
    }
    LocalDateTime now = now();
    LivePhotoRequestUser row = new LivePhotoRequestUser();
    row.setRequestId(open.getId());
    row.setUserId(userId);
    row.setCreatedAt(now);
    try {
      requestUserMapper.insert(row);
    } catch (DuplicateKeyException ex) {
      return new LivePhotoApplyResponse(countOf(reload(open.getId())), true);
    }
    int count = countOf(open);
    open.setApplicantCount(count);
    open.setLastRequestedAt(now);
    open.setUpdatedAt(now);
    requestMapper.updateById(open);
    return new LivePhotoApplyResponse(count, true);
  }

  public List<LivePhotoRequestItemResponse> listOpen() {
    long storeId = UserContext.storeId();
    return requestMapper.selectList(new LambdaQueryWrapper<LivePhotoRequest>()
            .eq(LivePhotoRequest::getStoreId, storeId)
            .orderByDesc(LivePhotoRequest::getLastRequestedAt)
            .orderByDesc(LivePhotoRequest::getId))
        .stream()
        .map(req -> {
          Product product = productMapper.selectById(req.getProductId());
          return LivePhotoRequestItemResponse.from(req, product == null ? null : product.getName());
        })
        .toList();
  }

  public void clearOpen(long productId) {
    LivePhotoRequest open = openRequest(productId);
    if (open == null) {
      return;
    }
    requestUserMapper.delete(new LambdaQueryWrapper<LivePhotoRequestUser>()
        .eq(LivePhotoRequestUser::getRequestId, open.getId()));
    requestMapper.deleteById(open.getId());
  }

  private void fillRequest(ProductResponse response, Long productId, Long userId) {
    if (productId == null) {
      response.setLivePhotoApplicantCount(0);
      response.setLivePhotoRequestedByMe(false);
      return;
    }
    LivePhotoRequest open = openRequest(productId);
    if (open == null) {
      response.setLivePhotoApplicantCount(0);
      response.setLivePhotoRequestedByMe(false);
      return;
    }
    response.setLivePhotoApplicantCount(countOf(open));
    if (userId == null) {
      response.setLivePhotoRequestedByMe(false);
      return;
    }
    Long mine = requestUserMapper.selectCount(new LambdaQueryWrapper<LivePhotoRequestUser>()
        .eq(LivePhotoRequestUser::getRequestId, open.getId())
        .eq(LivePhotoRequestUser::getUserId, userId));
    response.setLivePhotoRequestedByMe(mine != null && mine > 0);
  }

  private LivePhotoRequest insertOpen(long storeId, long productId) {
    LocalDateTime now = now();
    LivePhotoRequest created = new LivePhotoRequest();
    created.setStoreId(storeId);
    created.setProductId(productId);
    created.setApplicantCount(0);
    created.setLastRequestedAt(now);
    created.setCreatedAt(now);
    created.setUpdatedAt(now);
    try {
      requestMapper.insert(created);
      return created;
    } catch (DuplicateKeyException ex) {
      LivePhotoRequest existing = openRequest(productId);
      if (existing == null) {
        throw ex;
      }
      return existing;
    }
  }

  private LivePhotoRequest openRequest(long productId) {
    return requestMapper.selectOne(new LambdaQueryWrapper<LivePhotoRequest>()
        .eq(LivePhotoRequest::getProductId, productId)
        .last("LIMIT 1"));
  }

  private LivePhotoRequest reload(long id) {
    return requestMapper.selectById(id);
  }

  private int countOf(LivePhotoRequest request) {
    if (request == null) {
      return 0;
    }
    Long n = requestUserMapper.selectCount(new LambdaQueryWrapper<LivePhotoRequestUser>()
        .eq(LivePhotoRequestUser::getRequestId, request.getId()));
    return n == null ? 0 : n.intValue();
  }

  private boolean isExpired(Product product) {
    if (!StringUtils.hasText(product.getLivePhotoUrl()) || product.getLivePhotoTakenAt() == null) {
      return false;
    }
    int ttl = ttlMinutes(product.getStoreId());
    return !product.getLivePhotoTakenAt().plusMinutes(ttl).isAfter(now());
  }

  private int ttlMinutes(long storeId) {
    Store store = storeMapper.selectById(storeId);
    if (store == null || store.getPickImageTtlMinutes() == null || store.getPickImageTtlMinutes() < 1) {
      return 15;
    }
    return store.getPickImageTtlMinutes();
  }

  private static String blankToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }
}
