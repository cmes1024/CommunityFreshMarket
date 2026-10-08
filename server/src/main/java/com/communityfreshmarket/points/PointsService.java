package com.communityfreshmarket.points;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.communityfreshmarket.auth.UserContext;
import com.communityfreshmarket.common.BizException;
import com.communityfreshmarket.common.ErrorCodes;
import com.communityfreshmarket.checkin.CheckinService;
import com.communityfreshmarket.domain.Order;
import com.communityfreshmarket.domain.PointAccount;
import com.communityfreshmarket.domain.Product;
import com.communityfreshmarket.domain.PointLedger;
import com.communityfreshmarket.domain.PointRedeem;
import com.communityfreshmarket.domain.PointRedeemItem;
import com.communityfreshmarket.domain.Store;
import com.communityfreshmarket.domain.User;
import com.communityfreshmarket.mapper.PointAccountMapper;
import com.communityfreshmarket.mapper.PointLedgerMapper;
import com.communityfreshmarket.mapper.PointRedeemItemMapper;
import com.communityfreshmarket.mapper.PointRedeemMapper;
import com.communityfreshmarket.mapper.ProductMapper;
import com.communityfreshmarket.mapper.StoreMapper;
import com.communityfreshmarket.mapper.UserMapper;
import com.communityfreshmarket.points.dto.CounterPointRequest;
import com.communityfreshmarket.points.dto.CounterPointResponse;
import com.communityfreshmarket.points.dto.PointLedgerItemResponse;
import com.communityfreshmarket.points.dto.PointRedeemItemResponse;
import com.communityfreshmarket.points.dto.PointRedeemItemSaveRequest;
import com.communityfreshmarket.points.dto.PointRedeemRequest;
import com.communityfreshmarket.points.dto.PointRedeemResponse;
import com.communityfreshmarket.points.dto.PointsMeResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PointsService {

  private final PointAccountMapper accountMapper;
  private final PointLedgerMapper ledgerMapper;
  private final PointRedeemMapper redeemMapper;
  private final PointRedeemItemMapper itemMapper;
  private final ProductMapper productMapper;
  private final StoreMapper storeMapper;
  private final UserMapper userMapper;
  private final CheckinService checkinService;
  private final MemberCodeService memberCodeService;

  public PointsService(
      PointAccountMapper accountMapper,
      PointLedgerMapper ledgerMapper,
      PointRedeemMapper redeemMapper,
      PointRedeemItemMapper itemMapper,
      ProductMapper productMapper,
      StoreMapper storeMapper,
      UserMapper userMapper,
      CheckinService checkinService,
      MemberCodeService memberCodeService) {
    this.accountMapper = accountMapper;
    this.ledgerMapper = ledgerMapper;
    this.redeemMapper = redeemMapper;
    this.itemMapper = itemMapper;
    this.productMapper = productMapper;
    this.storeMapper = storeMapper;
    this.userMapper = userMapper;
    this.checkinService = checkinService;
    this.memberCodeService = memberCodeService;
  }

  public PointsMeResponse me() {
    long userId = UserContext.requireUserId();
    long storeId = UserContext.storeId();
    PointAccount account = findAccount(userId, storeId);
    int balance = account == null || account.getBalance() == null ? 0 : account.getBalance();
    List<PointLedgerItemResponse> recent = ledgerMapper.selectList(new LambdaQueryWrapper<PointLedger>()
            .eq(PointLedger::getStoreId, storeId)
            .eq(PointLedger::getUserId, userId)
            .orderByDesc(PointLedger::getId)
            .last("LIMIT 30"))
        .stream()
        .map(PointLedgerItemResponse::from)
        .toList();
    List<PointRedeemResponse> redeems = redeemMapper.selectList(new LambdaQueryWrapper<PointRedeem>()
            .eq(PointRedeem::getStoreId, storeId)
            .eq(PointRedeem::getUserId, userId)
            .orderByDesc(PointRedeem::getId)
            .last("LIMIT 20"))
        .stream()
        .map(PointRedeemResponse::from)
        .toList();
    Store store = storeMapper.selectById(storeId);
    int ratio = store == null || store.getPointsPerYuan() == null ? 1 : store.getPointsPerYuan();
    List<PointRedeemItemResponse> items = enabledItems(storeId);
    return new PointsMeResponse(balance, ratio, recent, redeems, items);
  }

  @Transactional
  public void awardOrderComplete(Order order) {
    if (order == null || order.getUserId() == null || order.getTotalAmountFen() == null) {
      return;
    }
    // 幂等：同订单只入账一次
    Long exists = ledgerMapper.selectCount(new LambdaQueryWrapper<PointLedger>()
        .eq(PointLedger::getStoreId, order.getStoreId())
        .eq(PointLedger::getReason, "ORDER_COMPLETE")
        .eq(PointLedger::getRefType, "ORDER")
        .eq(PointLedger::getRefId, order.getId()));
    if (exists != null && exists > 0) {
      return;
    }
    Store store = storeMapper.selectById(order.getStoreId());
    int ratio = store == null || store.getPointsPerYuan() == null ? 1 : store.getPointsPerYuan();
    if (ratio <= 0) {
      return;
    }
    int discount = order.getDiscountFen() == null ? 0 : order.getDiscountFen();
    int payable = Math.max(0, order.getTotalAmountFen() - discount);
    int earn = (payable / 100) * ratio;
    if (earn <= 0) {
      return;
    }
    applyDelta(
        order.getUserId(),
        order.getStoreId(),
        earn,
        "ORDER_COMPLETE",
        "ORDER",
        order.getId(),
        "预约单完成");
  }

  @Transactional
  public void awardCashierComplete(long userId, long storeId, long saleId, int payFen) {
    Long exists = ledgerMapper.selectCount(new LambdaQueryWrapper<PointLedger>()
        .eq(PointLedger::getStoreId, storeId)
        .eq(PointLedger::getReason, "CASHIER_COMPLETE")
        .eq(PointLedger::getRefType, "CASHIER")
        .eq(PointLedger::getRefId, saleId));
    if (exists != null && exists > 0) {
      return;
    }
    Store store = storeMapper.selectById(storeId);
    int ratio = store == null || store.getPointsPerYuan() == null ? 1 : store.getPointsPerYuan();
    if (ratio <= 0) {
      return;
    }
    int earn = (payFen / 100) * ratio;
    if (earn <= 0) {
      return;
    }
    applyDelta(userId, storeId, earn, "CASHIER_COMPLETE", "CASHIER", saleId, "到店收银完成");
  }

  @Transactional
  public CounterPointResponse awardCounter(CounterPointRequest request) {
    long storeId = UserContext.storeId();
    if (request == null || request.getAmountFen() == null || request.getAmountFen() <= 0) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "请填写消费金额");
    }
    int amountFen = request.getAmountFen();
    if (amountFen > 100_000_000) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "金额过大");
    }
    Long checkinId = request.getCheckinId();
    long userId;
    if (checkinId != null) {
      userId = checkinService.activeUserId(checkinId, storeId);
    } else {
      userId = memberCodeService.parseUserId(request.getCode(), storeId);
    }
    User user = userMapper.selectById(userId);
    if (user == null) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "会员不存在");
    }
    Store store = storeMapper.selectById(storeId);
    int ratio = store == null || store.getPointsPerYuan() == null ? 1 : store.getPointsPerYuan();
    if (ratio <= 0) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "店铺未开启积分");
    }
    int earn = (amountFen / 100) * ratio;
    if (earn <= 0) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "不满 1 元，记不到积分");
    }
    if (checkinId != null) {
      checkinService.consume(checkinId, storeId);
    } else {
      checkinService.consumeActiveForUser(storeId, userId);
    }
    String yuan = String.format(Locale.CHINA, "%.2f", amountFen / 100.0);
    applyDelta(userId, storeId, earn, "COUNTER_SPEND", "COUNTER", checkinId, "¥" + yuan);
    PointAccount account = findAccount(userId, storeId);
    int balance = account == null || account.getBalance() == null ? 0 : account.getBalance();
    CounterPointResponse response = new CounterPointResponse();
    response.setUserId(userId);
    response.setNickname(user.getNickname());
    response.setAmountFen(amountFen);
    response.setPoints(earn);
    response.setBalance(balance);
    return response;
  }

  public List<PointRedeemItemResponse> merchantItems() {
    long storeId = UserContext.storeId();
    return itemMapper.selectList(new LambdaQueryWrapper<PointRedeemItem>()
            .eq(PointRedeemItem::getStoreId, storeId)
            .orderByDesc(PointRedeemItem::getId))
        .stream()
        .map(PointRedeemItemResponse::from)
        .toList();
  }

  @Transactional
  public PointRedeemItemResponse createItem(PointRedeemItemSaveRequest request) {
    long storeId = UserContext.storeId();
    LocalDateTime now = LocalDateTime.now();
    PointRedeemItem item = new PointRedeemItem();
    item.setStoreId(storeId);
    fillItem(item, request, storeId);
    item.setCreatedAt(now);
    item.setUpdatedAt(now);
    itemMapper.insert(item);
    return PointRedeemItemResponse.from(item);
  }

  @Transactional
  public PointRedeemItemResponse updateItem(long id, PointRedeemItemSaveRequest request) {
    PointRedeemItem item = requireItem(id);
    fillItem(item, request, item.getStoreId());
    item.setUpdatedAt(LocalDateTime.now());
    itemMapper.updateById(item);
    if (item.getProductId() == null) {
      itemMapper.update(
          null,
          new LambdaUpdateWrapper<PointRedeemItem>()
              .eq(PointRedeemItem::getId, item.getId())
              .set(PointRedeemItem::getProductId, null));
    }
    return PointRedeemItemResponse.from(item);
  }

  @Transactional
  public void deleteItem(long id) {
    PointRedeemItem item = requireItem(id);
    itemMapper.deleteById(item.getId());
  }

  @Transactional
  public PointRedeemResponse redeem(PointRedeemRequest request) {
    long userId = UserContext.requireUserId();
    long storeId = UserContext.storeId();
    if (request == null || request.getItemId() == null) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "请选择兑换物");
    }
    PointRedeemItem item = itemMapper.selectById(request.getItemId());
    if (item == null || !item.getStoreId().equals(storeId) || !Boolean.TRUE.equals(item.getEnabled())) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "兑换物不可用");
    }
    int cost = item.getCostPoints() == null ? 0 : item.getCostPoints();
    if (cost <= 0) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "兑换物不可用");
    }
    applyDelta(userId, storeId, -cost, "REDEEM", "REDEEM", null, item.getTitle());

    LocalDateTime now = LocalDateTime.now();
    PointRedeem redeem = new PointRedeem();
    redeem.setStoreId(storeId);
    redeem.setUserId(userId);
    redeem.setItemId(item.getId());
    redeem.setCostPoints(cost);
    redeem.setTitle(item.getTitle());
    redeem.setStatus("PENDING");
    redeem.setCreatedAt(now);
    redeem.setUpdatedAt(now);
    redeemMapper.insert(redeem);

    // 回填 ledger ref
    PointLedger last = ledgerMapper.selectOne(new LambdaQueryWrapper<PointLedger>()
        .eq(PointLedger::getStoreId, storeId)
        .eq(PointLedger::getUserId, userId)
        .eq(PointLedger::getReason, "REDEEM")
        .orderByDesc(PointLedger::getId)
        .last("LIMIT 1"));
    if (last != null) {
      last.setRefId(redeem.getId());
      ledgerMapper.updateById(last);
    }
    return PointRedeemResponse.from(redeem);
  }

  public List<PointRedeemResponse> merchantPendingRedeems() {
    long storeId = UserContext.storeId();
    return redeemMapper.selectList(new LambdaQueryWrapper<PointRedeem>()
            .eq(PointRedeem::getStoreId, storeId)
            .eq(PointRedeem::getStatus, "PENDING")
            .orderByAsc(PointRedeem::getId))
        .stream()
        .map(PointRedeemResponse::from)
        .toList();
  }

  @Transactional
  public PointRedeemResponse completeRedeem(long id) {
    long storeId = UserContext.storeId();
    PointRedeem redeem = redeemMapper.selectById(id);
    if (redeem == null || !redeem.getStoreId().equals(storeId)) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "兑换单不存在");
    }
    if (!"PENDING".equals(redeem.getStatus())) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "兑换单已处理");
    }
    redeem.setStatus("DONE");
    redeem.setUpdatedAt(LocalDateTime.now());
    redeemMapper.updateById(redeem);
    return PointRedeemResponse.from(redeem);
  }

  @Transactional
  public PointRedeemResponse cancelRedeem(long id) {
    long storeId = UserContext.storeId();
    PointRedeem redeem = redeemMapper.selectById(id);
    if (redeem == null || !redeem.getStoreId().equals(storeId)) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "兑换单不存在");
    }
    if (!"PENDING".equals(redeem.getStatus())) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "兑换单已处理");
    }
    redeem.setStatus("CANCELLED");
    redeem.setUpdatedAt(LocalDateTime.now());
    redeemMapper.updateById(redeem);
    applyDelta(
        redeem.getUserId(),
        storeId,
        redeem.getCostPoints(),
        "CANCEL_REVERSAL",
        "REDEEM",
        redeem.getId(),
        "兑换驳回退回");
    return PointRedeemResponse.from(redeem);
  }

  private List<PointRedeemItemResponse> enabledItems(long storeId) {
    return itemMapper.selectList(new LambdaQueryWrapper<PointRedeemItem>()
            .eq(PointRedeemItem::getStoreId, storeId)
            .eq(PointRedeemItem::getEnabled, true)
            .orderByAsc(PointRedeemItem::getCostPoints)
            .orderByDesc(PointRedeemItem::getId))
        .stream()
        .map(PointRedeemItemResponse::from)
        .toList();
  }

  private PointRedeemItem requireItem(long id) {
    long storeId = UserContext.storeId();
    PointRedeemItem item = itemMapper.selectById(id);
    if (item == null || !item.getStoreId().equals(storeId)) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "兑换物不存在");
    }
    return item;
  }

  private void fillItem(PointRedeemItem item, PointRedeemItemSaveRequest request, long storeId) {
    Long productId = request == null ? null : request.getProductId();
    String title = request == null || request.getTitle() == null ? "" : request.getTitle().trim();
    if (productId != null) {
      Product product = productMapper.selectById(productId);
      if (product == null || !Objects.equals(product.getStoreId(), storeId)) {
        throw new BizException(ErrorCodes.BAD_REQUEST, "商品不存在");
      }
      if (title.isEmpty() && product.getName() != null) {
        title = product.getName().trim();
      }
    }
    if (title.isEmpty()) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "请填写兑换物名称");
    }
    if (title.length() > 128) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "兑换物名称过长");
    }
    item.setProductId(productId);
    item.setTitle(title);
    item.setCostPoints(requireCost(request));
    item.setEnabled(request.getEnabled() == null || request.getEnabled());
  }

  private static int requireCost(PointRedeemItemSaveRequest request) {
    if (request == null || request.getCostPoints() == null || request.getCostPoints() <= 0) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "所需积分须大于 0");
    }
    return request.getCostPoints();
  }

  private void applyDelta(
      long userId, long storeId, int delta, String reason, String refType, Long refId, String remark) {
    PointAccount account = findAccount(userId, storeId);
    LocalDateTime now = LocalDateTime.now();
    int balance = account == null || account.getBalance() == null ? 0 : account.getBalance();
    int next = balance + delta;
    if (next < 0) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "积分不足");
    }
    if (account == null) {
      account = new PointAccount();
      account.setUserId(userId);
      account.setStoreId(storeId);
      account.setBalance(next);
      account.setUpdatedAt(now);
      accountMapper.insert(account);
    } else {
      account.setBalance(next);
      account.setUpdatedAt(now);
      accountMapper.update(
          account,
          new LambdaQueryWrapper<PointAccount>()
              .eq(PointAccount::getUserId, userId)
              .eq(PointAccount::getStoreId, storeId));
    }
    PointLedger ledger = new PointLedger();
    ledger.setStoreId(storeId);
    ledger.setUserId(userId);
    ledger.setDelta(delta);
    ledger.setBalanceAfter(next);
    ledger.setReason(reason);
    ledger.setRefType(refType);
    ledger.setRefId(refId);
    ledger.setRemark(remark);
    ledger.setCreatedAt(now);
    ledgerMapper.insert(ledger);
  }

  private PointAccount findAccount(long userId, long storeId) {
    return accountMapper.selectOne(new LambdaQueryWrapper<PointAccount>()
        .eq(PointAccount::getUserId, userId)
        .eq(PointAccount::getStoreId, storeId));
  }
}
