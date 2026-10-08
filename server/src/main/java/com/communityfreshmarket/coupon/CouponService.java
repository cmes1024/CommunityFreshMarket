package com.communityfreshmarket.coupon;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.communityfreshmarket.auth.UserContext;
import com.communityfreshmarket.common.BizException;
import com.communityfreshmarket.common.ErrorCodes;
import com.communityfreshmarket.domain.Coupon;
import com.communityfreshmarket.domain.CouponClaim;
import com.communityfreshmarket.mapper.CouponClaimMapper;
import com.communityfreshmarket.mapper.CouponMapper;
import com.communityfreshmarket.coupon.dto.CouponClaimResponse;
import com.communityfreshmarket.coupon.dto.CouponResponse;
import com.communityfreshmarket.coupon.dto.CouponSaveRequest;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CouponService {

  private final CouponMapper couponMapper;
  private final CouponClaimMapper claimMapper;

  public CouponService(CouponMapper couponMapper, CouponClaimMapper claimMapper) {
    this.couponMapper = couponMapper;
    this.claimMapper = claimMapper;
  }

  public List<CouponResponse> listEnabled() {
    long storeId = UserContext.storeId();
    LocalDateTime now = LocalDateTime.now();
    return couponMapper.selectList(new LambdaQueryWrapper<Coupon>()
            .eq(Coupon::getStoreId, storeId)
            .eq(Coupon::getEnabled, true)
            .orderByDesc(Coupon::getId))
        .stream()
        .filter(c -> (c.getStartsAt() == null || !c.getStartsAt().isAfter(now))
            && (c.getEndsAt() == null || !c.getEndsAt().isBefore(now)))
        .map(CouponResponse::from)
        .toList();
  }

  public List<CouponResponse> listMerchant() {
    long storeId = UserContext.storeId();
    return couponMapper.selectList(new LambdaQueryWrapper<Coupon>()
            .eq(Coupon::getStoreId, storeId)
            .orderByDesc(Coupon::getId))
        .stream()
        .map(CouponResponse::from)
        .toList();
  }

  @Transactional
  public CouponResponse create(CouponSaveRequest request) {
    long storeId = UserContext.storeId();
    validate(request);
    LocalDateTime now = LocalDateTime.now();
    Coupon c = new Coupon();
    c.setStoreId(storeId);
    apply(c, request);
    c.setClaimedCount(0);
    c.setCreatedAt(now);
    c.setUpdatedAt(now);
    couponMapper.insert(c);
    return CouponResponse.from(c);
  }

  @Transactional
  public CouponResponse update(long id, CouponSaveRequest request) {
    Coupon c = requireOwned(id);
    validate(request);
    apply(c, request);
    c.setUpdatedAt(LocalDateTime.now());
    couponMapper.updateById(c);
    return CouponResponse.from(c);
  }

  @Transactional
  public CouponClaimResponse claim(long couponId) {
    long userId = UserContext.requireUserId();
    long storeId = UserContext.storeId();
    Coupon coupon = requireOwned(couponId);
    if (!Boolean.TRUE.equals(coupon.getEnabled())) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "优惠券不可用");
    }
    LocalDateTime now = LocalDateTime.now();
    if (coupon.getStartsAt() != null && coupon.getStartsAt().isAfter(now)) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "优惠券未开始");
    }
    if (coupon.getEndsAt() != null && coupon.getEndsAt().isBefore(now)) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "优惠券已结束");
    }
    if (coupon.getTotalLimit() != null
        && coupon.getClaimedCount() != null
        && coupon.getClaimedCount() >= coupon.getTotalLimit()) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "优惠券已领完");
    }
    int perUser = coupon.getPerUserLimit() == null ? 1 : coupon.getPerUserLimit();
    Long mine = claimMapper.selectCount(new LambdaQueryWrapper<CouponClaim>()
        .eq(CouponClaim::getCouponId, couponId)
        .eq(CouponClaim::getUserId, userId));
    if (mine != null && mine >= perUser) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "已达领取上限");
    }
    CouponClaim claim = new CouponClaim();
    claim.setCouponId(couponId);
    claim.setStoreId(storeId);
    claim.setUserId(userId);
    claim.setStatus("UNUSED");
    claim.setClaimedAt(now);
    claimMapper.insert(claim);
    coupon.setClaimedCount((coupon.getClaimedCount() == null ? 0 : coupon.getClaimedCount()) + 1);
    coupon.setUpdatedAt(now);
    couponMapper.updateById(coupon);
    return CouponClaimResponse.from(claim, coupon);
  }

  public List<CouponClaimResponse> myClaims() {
    long userId = UserContext.requireUserId();
    long storeId = UserContext.storeId();
    return claimMapper.selectList(new LambdaQueryWrapper<CouponClaim>()
            .eq(CouponClaim::getStoreId, storeId)
            .eq(CouponClaim::getUserId, userId)
            .orderByDesc(CouponClaim::getId))
        .stream()
        .map(claim -> {
          Coupon coupon = couponMapper.selectById(claim.getCouponId());
          return CouponClaimResponse.from(claim, coupon);
        })
        .toList();
  }

  private Coupon requireOwned(long id) {
    long storeId = UserContext.storeId();
    Coupon c = couponMapper.selectById(id);
    if (c == null || !c.getStoreId().equals(storeId)) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "优惠券不存在");
    }
    return c;
  }

  private static void validate(CouponSaveRequest request) {
    if (request == null || !StringUtils.hasText(request.getTitle()) || request.getFaceFen() == null) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "请填写券名称与面额");
    }
    String type = request.getType() == null ? "FIXED" : request.getType().trim().toUpperCase();
    if (!"FIXED".equals(type) && !"AMOUNT_OFF".equals(type)) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "券类型仅支持 FIXED / AMOUNT_OFF");
    }
    if (request.getFaceFen() <= 0) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "面额须大于 0");
    }
  }

  private static void apply(Coupon c, CouponSaveRequest request) {
    c.setTitle(request.getTitle().trim());
    c.setType(request.getType() == null ? "FIXED" : request.getType().trim().toUpperCase());
    c.setFaceFen(request.getFaceFen());
    c.setMinSpendFen(request.getMinSpendFen() == null ? 0 : request.getMinSpendFen());
    c.setTotalLimit(request.getTotalLimit());
    c.setPerUserLimit(request.getPerUserLimit() == null ? 1 : request.getPerUserLimit());
    c.setStartsAt(request.getStartsAt());
    c.setEndsAt(request.getEndsAt());
    c.setEnabled(request.getEnabled() == null || request.getEnabled());
  }
}
