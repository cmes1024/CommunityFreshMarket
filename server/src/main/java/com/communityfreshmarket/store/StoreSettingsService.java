package com.communityfreshmarket.store;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.communityfreshmarket.auth.UserContext;
import com.communityfreshmarket.catalog.StoreQueryService;
import com.communityfreshmarket.catalog.StoreSummaryResponse;
import com.communityfreshmarket.common.BizException;
import com.communityfreshmarket.common.ErrorCodes;
import com.communityfreshmarket.domain.Store;
import com.communityfreshmarket.mapper.StoreMapper;
import com.communityfreshmarket.points.PointsRatio;
import com.communityfreshmarket.store.dto.StoreUpdateRequest;
import com.communityfreshmarket.upload.UploadCleanup;
import java.util.List;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class StoreSettingsService {

  private final StoreMapper storeMapper;
  private final StoreQueryService storeQueryService;
  private final UploadCleanup uploadCleanup;

  public StoreSettingsService(
      StoreMapper storeMapper, StoreQueryService storeQueryService, UploadCleanup uploadCleanup) {
    this.storeMapper = storeMapper;
    this.storeQueryService = storeQueryService;
    this.uploadCleanup = uploadCleanup;
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
    if (request.getDeliveryRemarkPhrases() != null) {
      store.setDeliveryRemarkPhrases(RemarkPhrases.store(request.getDeliveryRemarkPhrases()));
    }
    if (request.getPickupRemarkPhrases() != null) {
      store.setPickupRemarkPhrases(RemarkPhrases.store(request.getPickupRemarkPhrases()));
    }
    if (request.getMinDeliveryFen() != null) {
      store.setMinDeliveryFen(request.getMinDeliveryFen());
    }
    boolean clearHero = false;
    String previousHero = store.getHeroImageUrl();
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
    if (request.getMemberCodeEntry() != null) {
      String entry = request.getMemberCodeEntry().trim().toUpperCase();
      if (!"PROFILE".equals(entry) && !"TAB".equals(entry)) {
        throw new BizException(ErrorCodes.BAD_REQUEST, "会员码入口仅支持我的或底栏");
      }
      store.setMemberCodeEntry(entry);
    }
    if (request.getPointsPerYuan() != null) {
      store.setPointsPerYuan(PointsRatio.require(request.getPointsPerYuan()));
    }
    if (request.getPickImageTtlMinutes() != null) {
      int ttl = request.getPickImageTtlMinutes();
      if (ttl < 1) {
        throw new BizException(ErrorCodes.BAD_REQUEST, "有效时间至少 1 分钟");
      }
      store.setPickImageTtlMinutes(ttl);
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
    if (request.getHeroImageUrl() != null && !sameText(previousHero, store.getHeroImageUrl())) {
      uploadCleanup.release(previousHero == null ? List.of() : List.of(previousHero));
    }
    return storeQueryService.currentStore();
  }

  private static boolean sameText(String left, String right) {
    String a = StringUtils.hasText(left) ? left.trim() : "";
    String b = StringUtils.hasText(right) ? right.trim() : "";
    return a.equals(b);
  }

  private static String blankToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }
}
