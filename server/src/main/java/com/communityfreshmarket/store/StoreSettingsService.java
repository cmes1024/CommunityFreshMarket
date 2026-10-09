package com.communityfreshmarket.store;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.communityfreshmarket.auth.UserContext;
import com.communityfreshmarket.catalog.ProductImageUrls;
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
    boolean clearHeroUrls = false;
    List<String> removedHeroes = List.of();
    if (request.getHeroImageUrls() != null) {
      removedHeroes = applyHeroes(store, ProductImageUrls.normalize(request.getHeroImageUrls()));
      clearHero = store.getHeroImageUrl() == null;
      clearHeroUrls = store.getHeroImageUrls() == null;
    } else if (request.getHeroImageUrl() != null) {
      String hero = blankToNull(request.getHeroImageUrl());
      List<String> next = hero == null ? List.of() : ProductImageUrls.normalize(List.of(hero));
      removedHeroes = applyHeroes(store, next);
      clearHero = store.getHeroImageUrl() == null;
      clearHeroUrls = store.getHeroImageUrls() == null;
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
    boolean clearNotice = false;
    if (request.getHomeNotice() != null) {
      String notice = blankToNull(request.getHomeNotice());
      if (notice != null && notice.length() > 80) {
        throw new BizException(ErrorCodes.BAD_REQUEST, "公告最多 80 个字");
      }
      store.setHomeNotice(notice);
      clearNotice = notice == null;
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
    if (clearHero || clearHeroUrls || clearKicker || clearTitle || clearSubtitle || clearNotice) {
      LambdaUpdateWrapper<Store> clear =
          new LambdaUpdateWrapper<Store>().eq(Store::getId, storeId);
      if (clearHero) {
        clear.set(Store::getHeroImageUrl, null);
      }
      if (clearHeroUrls) {
        clear.set(Store::getHeroImageUrls, null);
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
      if (clearNotice) {
        clear.set(Store::getHomeNotice, null);
      }
      storeMapper.update(null, clear);
    }
    if (!removedHeroes.isEmpty()) {
      uploadCleanup.release(removedHeroes);
    }
    return storeQueryService.currentStore();
  }

  private static List<String> applyHeroes(Store store, List<String> next) {
    List<String> previous = ProductImageUrls.resolve(store.getHeroImageUrls(), store.getHeroImageUrl());
    store.setHeroImageUrls(ProductImageUrls.toJson(next));
    store.setHeroImageUrl(ProductImageUrls.cover(next));
    return previous.stream().filter(url -> next == null || !next.contains(url)).toList();
  }

  private static String blankToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }
}
