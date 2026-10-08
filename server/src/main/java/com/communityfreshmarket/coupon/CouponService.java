package com.communityfreshmarket.coupon;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    disableExpired(storeId);
    LocalDateTime now = LocalDateTime.now();
    return couponMapper.selectList(new LambdaQueryWrapper<Coupon>()
        .eq(Coupon::getStoreId, storeId)
        .eq(Coupon::getEnabled, true)
        .eq(Coupon::getRemoved, false)
            .orderByDesc(Coupon::getId))
        .stream()
        .filter(c -> inClaimWindow(c, now))
        .map(CouponResponse::from)
        .toList();
  }

  public List<CouponResponse> listMerchant() {
    long storeId = UserContext.storeId();
    disableExpired(storeId);
    List<CouponResponse> rows = couponMapper.selectList(new LambdaQueryWrapper<Coupon>()
            .eq(Coupon::getStoreId, storeId)
            .eq(Coupon::getRemoved, false)
            .orderByDesc(Coupon::getId))
        .stream()
        .map(CouponResponse::from)
        .toList();
    fillClaimStats(rows);
    return rows;
  }

  @Transactional
  public CouponResponse create(CouponSaveRequest request) {
    long storeId = UserContext.storeId();
    validate(request);
    LocalDateTime now = LocalDateTime.now();
    if (request.getEndsAt() != null && !request.getEndsAt().isAfter(now)) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "使用截止须晚于现在");
    }
    if (request.getClaimEndsAt() != null && !request.getClaimEndsAt().isAfter(now)) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "领取截止须晚于现在");
    }
    Coupon c = new Coupon();
    c.setStoreId(storeId);
    apply(c, request);
    c.setClaimedCount(0);
    c.setRemoved(false);
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
    LocalDateTime now = LocalDateTime.now();
    if (c.getEndsAt() != null && c.getEndsAt().isBefore(now)) {
      c.setEnabled(false);
      expireUnusedClaims(c.getId());
    }
    c.setUpdatedAt(now);
    couponMapper.updateById(c);
    return CouponResponse.from(c);
  }

  @Transactional
  public void delete(long id) {
    Coupon c = requireOwned(id);
    Long reserved = claimMapper.selectCount(new LambdaQueryWrapper<CouponClaim>()
        .eq(CouponClaim::getCouponId, c.getId())
        .eq(CouponClaim::getStatus, "RESERVED"));
    if (reserved != null && reserved > 0) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "这张券正在订单或收银中使用，不能删除");
    }
    claimMapper.delete(new LambdaQueryWrapper<CouponClaim>()
        .eq(CouponClaim::getCouponId, c.getId())
        .in(CouponClaim::getStatus, List.of("UNUSED", "EXPIRED")));
    c.setRemoved(true);
    c.setEnabled(false);
    c.setUpdatedAt(LocalDateTime.now());
    couponMapper.updateById(c);
  }

  @Transactional
  public CouponClaimResponse claim(long couponId) {
    long userId = UserContext.requireUserId();
    long storeId = UserContext.storeId();
    disableExpired(storeId);
    Coupon coupon = requireOwned(couponId);
    LocalDateTime now = LocalDateTime.now();
    if (coupon.getEndsAt() != null && coupon.getEndsAt().isBefore(now)) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "优惠券已过期");
    }
    if (!Boolean.TRUE.equals(coupon.getEnabled())) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "优惠券不可用");
    }
    if (coupon.getStartsAt() != null && coupon.getStartsAt().isAfter(now)) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "领取未开始");
    }
    if (coupon.getClaimEndsAt() != null && coupon.getClaimEndsAt().isBefore(now)) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "领取已结束");
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
    disableExpired(storeId);
    return claimsOf(storeId, userId);
  }

  public List<CouponClaimResponse> usableForCashier(long userId, Long saleId) {
    long storeId = UserContext.storeId();
    disableExpired(storeId);
    return claimMapper.selectList(new LambdaQueryWrapper<CouponClaim>()
            .eq(CouponClaim::getStoreId, storeId)
            .eq(CouponClaim::getUserId, userId)
            .orderByDesc(CouponClaim::getId))
        .stream()
        .filter(claim -> "UNUSED".equals(claim.getStatus())
            || ("RESERVED".equals(claim.getStatus())
                && saleId != null
                && saleId.equals(claim.getCashierId())))
        .map(claim -> CouponClaimResponse.from(claim, couponMapper.selectById(claim.getCouponId())))
        .toList();
  }

  /** 下单挂券：UNUSED → RESERVED，返回本次抵扣分。 */
  @Transactional
  public int reserveForOrder(long claimId, long userId, long storeId, long orderId, int spendFen) {
    CouponClaim claim = requireUnused(claimId, userId, storeId);
    Coupon coupon = requireUsableCoupon(claim.getCouponId(), storeId);
    int discount = discountFen(coupon, spendFen);
    int updated = claimMapper.update(
        null,
        new LambdaUpdateWrapper<CouponClaim>()
            .eq(CouponClaim::getId, claim.getId())
            .eq(CouponClaim::getStatus, "UNUSED")
            .set(CouponClaim::getStatus, "RESERVED")
            .set(CouponClaim::getOrderId, orderId)
            .set(CouponClaim::getCashierId, null));
    if (updated == 0) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "优惠券不可用");
    }
    return discount;
  }

  @Transactional
  public int reserveForCashier(long claimId, long userId, long storeId, long saleId, int spendFen) {
    CouponClaim claim = requireUnused(claimId, userId, storeId);
    Coupon coupon = requireUsableCoupon(claim.getCouponId(), storeId);
    int discount = discountFen(coupon, spendFen);
    int updated = claimMapper.update(
        null,
        new LambdaUpdateWrapper<CouponClaim>()
            .eq(CouponClaim::getId, claim.getId())
            .eq(CouponClaim::getStatus, "UNUSED")
            .set(CouponClaim::getStatus, "RESERVED")
            .set(CouponClaim::getCashierId, saleId)
            .set(CouponClaim::getOrderId, null));
    if (updated == 0) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "优惠券不可用");
    }
    return discount;
  }

  /** 合计变化后重算收银券抵扣；未满门槛则释放。 */
  @Transactional
  public int syncCashierDiscount(Long claimId, int spendFen) {
    if (claimId == null) {
      return 0;
    }
    CouponClaim claim = claimMapper.selectById(claimId);
    if (claim == null || !"RESERVED".equals(claim.getStatus())) {
      return 0;
    }
    Coupon coupon = couponMapper.selectById(claim.getCouponId());
    if (coupon == null) {
      releaseClaim(claim.getId());
      return 0;
    }
    int min = coupon.getMinSpendFen() == null ? 0 : coupon.getMinSpendFen();
    if (spendFen < min) {
      releaseClaim(claim.getId());
      return -1;
    }
    int face = coupon.getFaceFen() == null ? 0 : coupon.getFaceFen();
    return Math.min(Math.max(face, 0), spendFen);
  }

  @Transactional
  public void releaseClaim(long claimId) {
    claimMapper.update(
        null,
        new LambdaUpdateWrapper<CouponClaim>()
            .eq(CouponClaim::getId, claimId)
            .eq(CouponClaim::getStatus, "RESERVED")
            .set(CouponClaim::getStatus, "UNUSED")
            .set(CouponClaim::getOrderId, null)
            .set(CouponClaim::getCashierId, null));
  }

  @Transactional
  public void releaseByOrder(long orderId) {
    claimMapper.update(
        null,
        new LambdaUpdateWrapper<CouponClaim>()
            .eq(CouponClaim::getOrderId, orderId)
            .eq(CouponClaim::getStatus, "RESERVED")
            .set(CouponClaim::getStatus, "UNUSED")
            .set(CouponClaim::getOrderId, null));
  }

  @Transactional
  public void useByOrder(long orderId) {
    claimMapper.update(
        null,
        new LambdaUpdateWrapper<CouponClaim>()
            .eq(CouponClaim::getOrderId, orderId)
            .eq(CouponClaim::getStatus, "RESERVED")
            .set(CouponClaim::getStatus, "USED")
            .set(CouponClaim::getUsedAt, LocalDateTime.now()));
  }

  @Transactional
  public void useByCashier(long saleId) {
    claimMapper.update(
        null,
        new LambdaUpdateWrapper<CouponClaim>()
            .eq(CouponClaim::getCashierId, saleId)
            .eq(CouponClaim::getStatus, "RESERVED")
            .set(CouponClaim::getStatus, "USED")
            .set(CouponClaim::getUsedAt, LocalDateTime.now()));
  }

  private List<CouponClaimResponse> claimsOf(long storeId, long userId) {
    LocalDateTime now = LocalDateTime.now();
    return claimMapper.selectList(new LambdaQueryWrapper<CouponClaim>()
            .eq(CouponClaim::getStoreId, storeId)
            .eq(CouponClaim::getUserId, userId)
            .orderByDesc(CouponClaim::getId))
        .stream()
        .map(claim -> {
          Coupon coupon = couponMapper.selectById(claim.getCouponId());
          if ("UNUSED".equals(claim.getStatus())
              && coupon != null
              && coupon.getEndsAt() != null
              && coupon.getEndsAt().isBefore(now)) {
            claim.setStatus("EXPIRED");
            claimMapper.updateById(claim);
          }
          return CouponClaimResponse.from(claim, coupon);
        })
        .toList();
  }

  private CouponClaim requireUnused(long claimId, long userId, long storeId) {
    CouponClaim claim = claimMapper.selectById(claimId);
    if (claim == null
        || !claim.getStoreId().equals(storeId)
        || !claim.getUserId().equals(userId)
        || !"UNUSED".equals(claim.getStatus())) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "优惠券不可用");
    }
    return claim;
  }

  private Coupon requireUsableCoupon(long couponId, long storeId) {
    disableExpired(storeId);
    Coupon coupon = couponMapper.selectById(couponId);
    LocalDateTime now = LocalDateTime.now();
    if (coupon != null && coupon.getEndsAt() != null && coupon.getEndsAt().isBefore(now)) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "优惠券已过期");
    }
    if (coupon == null || !coupon.getStoreId().equals(storeId) || Boolean.TRUE.equals(coupon.getRemoved())) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "优惠券不可用");
    }
    return coupon;
  }

  private static int discountFen(Coupon coupon, int spendFen) {
    int min = coupon.getMinSpendFen() == null ? 0 : coupon.getMinSpendFen();
    if (spendFen < min) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "未满优惠券使用门槛");
    }
    int face = coupon.getFaceFen() == null ? 0 : coupon.getFaceFen();
    if (face <= 0) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "优惠券不可用");
    }
    return Math.min(face, spendFen);
  }

  private void fillClaimStats(List<CouponResponse> rows) {
    if (rows.isEmpty()) {
      return;
    }
    List<Long> ids = rows.stream().map(CouponResponse::getId).toList();
    List<CouponClaim> claims = claimMapper.selectList(new LambdaQueryWrapper<CouponClaim>()
        .in(CouponClaim::getCouponId, ids));
    Map<Long, Set<Long>> users = new HashMap<>();
    Map<Long, Integer> unused = new HashMap<>();
    Map<Long, Integer> reserved = new HashMap<>();
    for (CouponClaim claim : claims) {
      users.computeIfAbsent(claim.getCouponId(), key -> new HashSet<>()).add(claim.getUserId());
      if ("UNUSED".equals(claim.getStatus())) {
        unused.merge(claim.getCouponId(), 1, Integer::sum);
      } else if ("RESERVED".equals(claim.getStatus())) {
        reserved.merge(claim.getCouponId(), 1, Integer::sum);
      }
    }
    for (CouponResponse row : rows) {
      Set<Long> people = users.get(row.getId());
      row.setClaimUserCount(people == null ? 0 : people.size());
      row.setUnusedCount(unused.getOrDefault(row.getId(), 0));
      row.setReservedCount(reserved.getOrDefault(row.getId(), 0));
    }
  }

  private void disableExpired(long storeId) {
    LocalDateTime now = LocalDateTime.now();
    List<Coupon> due = couponMapper.selectList(new LambdaQueryWrapper<Coupon>()
        .eq(Coupon::getStoreId, storeId)
        .eq(Coupon::getRemoved, false)
        .eq(Coupon::getEnabled, true)
        .isNotNull(Coupon::getEndsAt)
        .lt(Coupon::getEndsAt, now));
    if (due.isEmpty()) {
      return;
    }
    List<Long> ids = due.stream().map(Coupon::getId).toList();
    couponMapper.update(
        null,
        new LambdaUpdateWrapper<Coupon>()
            .in(Coupon::getId, ids)
            .set(Coupon::getEnabled, false)
            .set(Coupon::getUpdatedAt, now));
    claimMapper.update(
        null,
        new LambdaUpdateWrapper<CouponClaim>()
            .in(CouponClaim::getCouponId, ids)
            .eq(CouponClaim::getStatus, "UNUSED")
            .set(CouponClaim::getStatus, "EXPIRED"));
  }

  private void expireUnusedClaims(long couponId) {
    claimMapper.update(
        null,
        new LambdaUpdateWrapper<CouponClaim>()
            .eq(CouponClaim::getCouponId, couponId)
            .eq(CouponClaim::getStatus, "UNUSED")
            .set(CouponClaim::getStatus, "EXPIRED"));
  }

  private Coupon requireOwned(long id) {
    long storeId = UserContext.storeId();
    Coupon c = couponMapper.selectById(id);
    if (c == null || !c.getStoreId().equals(storeId) || Boolean.TRUE.equals(c.getRemoved())) {
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
    if (request.getTotalLimit() != null && request.getTotalLimit() < 1) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "发放张数须大于 0");
    }
    if (request.getStartsAt() != null
        && request.getClaimEndsAt() != null
        && request.getStartsAt().isAfter(request.getClaimEndsAt())) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "领取开始不能晚于领取截止");
    }
    if (request.getStartsAt() != null
        && request.getEndsAt() != null
        && request.getStartsAt().isAfter(request.getEndsAt())) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "领取开始不能晚于使用截止");
    }
    if (request.getClaimEndsAt() != null
        && request.getEndsAt() != null
        && request.getClaimEndsAt().isAfter(request.getEndsAt())) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "领取截止不能晚于使用截止");
    }
  }

  /** 领取窗口：未到开始、已过领取截止、或已过使用截止，都不能再领。 */
  private static boolean inClaimWindow(Coupon c, LocalDateTime now) {
    if (c.getStartsAt() != null && c.getStartsAt().isAfter(now)) {
      return false;
    }
    if (c.getClaimEndsAt() != null && c.getClaimEndsAt().isBefore(now)) {
      return false;
    }
    return c.getEndsAt() == null || !c.getEndsAt().isBefore(now);
  }

  private static void apply(Coupon c, CouponSaveRequest request) {
    c.setTitle(request.getTitle().trim());
    c.setType(request.getType() == null ? "FIXED" : request.getType().trim().toUpperCase());
    c.setFaceFen(request.getFaceFen());
    c.setMinSpendFen(request.getMinSpendFen() == null ? 0 : request.getMinSpendFen());
    c.setTotalLimit(request.getTotalLimit());
    c.setPerUserLimit(request.getPerUserLimit() == null ? 1 : request.getPerUserLimit());
    c.setStartsAt(request.getStartsAt());
    c.setClaimEndsAt(request.getClaimEndsAt());
    c.setEndsAt(request.getEndsAt());
    c.setEnabled(request.getEnabled() == null || request.getEnabled());
  }
}
