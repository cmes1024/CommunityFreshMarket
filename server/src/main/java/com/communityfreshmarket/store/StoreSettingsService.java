package com.communityfreshmarket.store;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.communityfreshmarket.auth.UserContext;
import com.communityfreshmarket.catalog.StoreQueryService;
import com.communityfreshmarket.catalog.StoreSummaryResponse;
import com.communityfreshmarket.common.BizException;
import com.communityfreshmarket.common.ErrorCodes;
import com.communityfreshmarket.domain.Store;
import com.communityfreshmarket.mapper.StoreMapper;
import com.communityfreshmarket.store.dto.StoreUpdateRequest;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class StoreSettingsService {

  private final StoreMapper storeMapper;
  private final StoreQueryService storeQueryService;

  public StoreSettingsService(StoreMapper storeMapper, StoreQueryService storeQueryService) {
    this.storeMapper = storeMapper;
    this.storeQueryService = storeQueryService;
  }

  @Transactional
  public StoreSummaryResponse update(StoreUpdateRequest request) {
    long storeId = UserContext.storeId();
    Store store = storeMapper.selectById(storeId);
    if (store == null) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "店铺不存在");
    }
    if (StringUtils.hasText(request.getName())) {
      store.setName(request.getName().trim());
    }
    if (request.getContactPhone() != null) {
      store.setContactPhone(blankToNull(request.getContactPhone()));
    }
    if (request.getContactNote() != null) {
      store.setContactNote(blankToNull(request.getContactNote()));
    }
    boolean clearHero = false;
    if (request.getHeroImageUrl() != null) {
      String hero = blankToNull(request.getHeroImageUrl());
      store.setHeroImageUrl(hero);
      clearHero = hero == null;
    }
    boolean clearKicker = false;
    if (request.getHomeKicker() != null) {
      String kicker = blankToNull(request.getHomeKicker());
      store.setHomeKicker(kicker);
      clearKicker = kicker == null;
    }
    boolean clearTitle = false;
    if (request.getHomeTitle() != null) {
      String title = blankToNull(request.getHomeTitle());
      store.setHomeTitle(title);
      clearTitle = title == null;
    }
    boolean clearSubtitle = false;
    if (request.getHomeSubtitle() != null) {
      String subtitle = blankToNull(request.getHomeSubtitle());
      store.setHomeSubtitle(subtitle);
      clearSubtitle = subtitle == null;
    }
    if (request.getHomeProductCols() != null) {
      int cols = request.getHomeProductCols();
      if (cols < 1 || cols > 3) {
        throw new BizException(ErrorCodes.BAD_REQUEST, "首页列数仅支持 1、2、3");
      }
      store.setHomeProductCols(cols);
    }
    if (request.getProductDetailMode() != null) {
      String mode = request.getProductDetailMode().trim().toUpperCase();
      if (!"SHEET".equals(mode) && !"PAGE".equals(mode)) {
        throw new BizException(ErrorCodes.BAD_REQUEST, "商品详情仅支持半屏或整页");
      }
      store.setProductDetailMode(mode);
    }
    store.setUpdatedAt(LocalDateTime.now());
    storeMapper.updateById(store);
    // updateById 默认跳过 null，清除需显式 set null
    if (clearHero || clearKicker || clearTitle || clearSubtitle) {
      LambdaUpdateWrapper<Store> clear =
          new LambdaUpdateWrapper<Store>().eq(Store::getId, storeId);
      if (clearHero) {
        clear.set(Store::getHeroImageUrl, null);
      }
      if (clearKicker) {
        clear.set(Store::getHomeKicker, null);
      }
      if (clearTitle) {
        clear.set(Store::getHomeTitle, null);
      }
      if (clearSubtitle) {
        clear.set(Store::getHomeSubtitle, null);
      }
      storeMapper.update(null, clear);
    }
    return storeQueryService.currentStore();
  }

  private static String blankToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }
}
