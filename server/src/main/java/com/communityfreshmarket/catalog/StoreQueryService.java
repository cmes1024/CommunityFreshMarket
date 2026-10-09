package com.communityfreshmarket.catalog;

import com.communityfreshmarket.auth.UserContext;
import com.communityfreshmarket.common.BizException;
import com.communityfreshmarket.common.ErrorCodes;
import com.communityfreshmarket.domain.Store;
import com.communityfreshmarket.mapper.StoreMapper;
import com.communityfreshmarket.points.PointsRatio;
import com.communityfreshmarket.store.RemarkPhrases;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class StoreQueryService {

  private final StoreMapper storeMapper;

  public StoreQueryService(StoreMapper storeMapper) {
    this.storeMapper = storeMapper;
  }

  public StoreSummaryResponse currentStore() {
    long storeId = UserContext.storeId();
    Store store = storeMapper.selectById(storeId);
    if (store == null || !"ACTIVE".equals(store.getStatus())) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "店铺不存在");
    }
    StoreSummaryResponse r = new StoreSummaryResponse();
    r.setId(store.getId());
    r.setName(store.getName());
    r.setContactPhone(store.getContactPhone());
    r.setContactNote(store.getContactNote());
    r.setDeliveryRemarkPhrases(RemarkPhrases.parse(store.getDeliveryRemarkPhrases()));
    r.setPickupRemarkPhrases(RemarkPhrases.parse(store.getPickupRemarkPhrases()));
    Integer minDelivery = store.getMinDeliveryFen();
    r.setMinDeliveryFen(minDelivery == null || minDelivery < 0 ? 0 : minDelivery);
    r.setHeroImageUrl(store.getHeroImageUrl());
    r.setHomeKicker(store.getHomeKicker());
    r.setHomeTitle(store.getHomeTitle());
    r.setHomeSubtitle(store.getHomeSubtitle());
    Integer cols = store.getHomeProductCols();
    r.setHomeProductCols(cols != null && cols >= 1 && cols <= 3 ? cols : 2);
    r.setProductDetailMode(normalizeDetailMode(store.getProductDetailMode()));
    r.setMemberCodeEntry(normalizeMemberCodeEntry(store.getMemberCodeEntry()));
    BigDecimal ratio = store.getPointsPerYuan();
    r.setPointsPerYuan(ratio == null || ratio.signum() < 0 ? BigDecimal.ONE : PointsRatio.plain(ratio));
    Integer ttl = store.getPickImageTtlMinutes();
    r.setPickImageTtlMinutes(ttl == null || ttl < 1 ? 15 : ttl);
    return r;
  }

  static String normalizeDetailMode(String mode) {
    return "PAGE".equalsIgnoreCase(mode) ? "PAGE" : "SHEET";
  }

  static String normalizeMemberCodeEntry(String entry) {
    return "TAB".equalsIgnoreCase(entry) ? "TAB" : "PROFILE";
  }
}
