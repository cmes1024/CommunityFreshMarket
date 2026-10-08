package com.communityfreshmarket.points;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.communityfreshmarket.auth.UserContext;
import com.communityfreshmarket.common.BizException;
import com.communityfreshmarket.common.ErrorCodes;
import com.communityfreshmarket.domain.Order;
import com.communityfreshmarket.domain.PointAccount;
import com.communityfreshmarket.domain.PointLedger;
import com.communityfreshmarket.domain.PointRedeem;
import com.communityfreshmarket.domain.Store;
import com.communityfreshmarket.mapper.PointAccountMapper;
import com.communityfreshmarket.mapper.PointLedgerMapper;
import com.communityfreshmarket.mapper.PointRedeemMapper;
import com.communityfreshmarket.mapper.StoreMapper;
import com.communityfreshmarket.points.dto.PointLedgerItemResponse;
import com.communityfreshmarket.points.dto.PointRedeemRequest;
import com.communityfreshmarket.points.dto.PointRedeemResponse;
import com.communityfreshmarket.points.dto.PointsMeResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PointsService {

  private final PointAccountMapper accountMapper;
  private final PointLedgerMapper ledgerMapper;
  private final PointRedeemMapper redeemMapper;
  private final StoreMapper storeMapper;

  public PointsService(
      PointAccountMapper accountMapper,
      PointLedgerMapper ledgerMapper,
      PointRedeemMapper redeemMapper,
      StoreMapper storeMapper) {
    this.accountMapper = accountMapper;
    this.ledgerMapper = ledgerMapper;
    this.redeemMapper = redeemMapper;
    this.storeMapper = storeMapper;
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
    return new PointsMeResponse(balance, ratio, recent, redeems);
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
    int earn = (order.getTotalAmountFen() / 100) * ratio;
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
  public PointRedeemResponse redeem(PointRedeemRequest request) {
    long userId = UserContext.requireUserId();
    long storeId = UserContext.storeId();
    if (request == null
        || request.getCostPoints() == null
        || request.getCostPoints() <= 0
        || !StringUtils.hasText(request.getTitle())) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "请填写兑换内容与积分");
    }
    String title = request.getTitle().trim();
    if (title.length() > 128) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "兑换说明过长");
    }
    applyDelta(
        userId,
        storeId,
        -request.getCostPoints(),
        "REDEEM",
        "REDEEM",
        null,
        title);

    LocalDateTime now = LocalDateTime.now();
    PointRedeem redeem = new PointRedeem();
    redeem.setStoreId(storeId);
    redeem.setUserId(userId);
    redeem.setCostPoints(request.getCostPoints());
    redeem.setTitle(title);
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
