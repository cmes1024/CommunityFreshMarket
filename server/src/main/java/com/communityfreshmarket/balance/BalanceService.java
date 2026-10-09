package com.communityfreshmarket.balance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.communityfreshmarket.auth.UserContext;
import com.communityfreshmarket.balance.dto.BalanceLedgerItemResponse;
import com.communityfreshmarket.balance.dto.BalanceMeResponse;
import com.communityfreshmarket.balance.dto.BalanceTopupRequest;
import com.communityfreshmarket.balance.dto.BalanceTopupResponse;
import com.communityfreshmarket.checkin.CheckinService;
import com.communityfreshmarket.common.BizException;
import com.communityfreshmarket.common.ErrorCodes;
import com.communityfreshmarket.domain.BalanceAccount;
import com.communityfreshmarket.domain.BalanceLedger;
import com.communityfreshmarket.domain.User;
import com.communityfreshmarket.mapper.BalanceAccountMapper;
import com.communityfreshmarket.mapper.BalanceLedgerMapper;
import com.communityfreshmarket.mapper.UserMapper;
import com.communityfreshmarket.points.MemberCodeService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BalanceService implements OrderBalance {

  private static final int MAX_FEN = 100_000_000;

  private final BalanceAccountMapper accountMapper;
  private final BalanceLedgerMapper ledgerMapper;
  private final UserMapper userMapper;
  private final MemberCodeService memberCodeService;
  private final CheckinService checkinService;

  public BalanceService(
      BalanceAccountMapper accountMapper,
      BalanceLedgerMapper ledgerMapper,
      UserMapper userMapper,
      MemberCodeService memberCodeService,
      CheckinService checkinService) {
    this.accountMapper = accountMapper;
    this.ledgerMapper = ledgerMapper;
    this.userMapper = userMapper;
    this.memberCodeService = memberCodeService;
    this.checkinService = checkinService;
  }

  public BalanceMeResponse mine() {
    long userId = UserContext.requireUserId();
    long storeId = UserContext.storeId();
    BalanceAccount account = findAccount(userId, storeId);
    int balance = account == null || account.getBalanceFen() == null ? 0 : account.getBalanceFen();
    List<BalanceLedgerItemResponse> recent = ledgerMapper.selectList(new LambdaQueryWrapper<BalanceLedger>()
            .eq(BalanceLedger::getStoreId, storeId)
            .eq(BalanceLedger::getUserId, userId)
            .orderByDesc(BalanceLedger::getId)
            .last("LIMIT 20"))
        .stream()
        .map(BalanceLedgerItemResponse::from)
        .toList();
    return new BalanceMeResponse(balance, recent);
  }

  @Transactional
  public BalanceTopupResponse topUp(BalanceTopupRequest request) {
    long storeId = UserContext.storeId();
    long operatorId = UserContext.requireUserId();
    int amountFen = requireAmount(request);
    Long checkinId = request.getCheckinId();
    long userId = checkinId != null
        ? checkinService.activeUserId(checkinId, storeId)
        : memberCodeService.parseUserId(request.getCode(), storeId);
    User user = userMapper.selectById(userId);
    if (user == null) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "会员不存在");
    }
    String yuan = String.format(Locale.CHINA, "%.2f", amountFen / 100.0);
    int balance = applyDelta(
        userId, storeId, amountFen, "TOPUP", "TOPUP", checkinId, operatorId, "现金 ¥" + yuan);
    if (checkinId != null) {
      checkinService.consume(checkinId, storeId);
    } else {
      checkinService.consumeActiveForUser(storeId, userId);
    }
    BalanceTopupResponse response = new BalanceTopupResponse();
    response.setUserId(userId);
    response.setNickname(user.getNickname());
    response.setAmountFen(amountFen);
    response.setBalanceFen(balance);
    return response;
  }

  @Override
  @Transactional
  public int apply(long orderId, long userId, long storeId, int payableFen, boolean use) {
    if (!use || payableFen <= 0) {
      return 0;
    }
    BalanceAccount account = findAccount(userId, storeId);
    int balance = account == null || account.getBalanceFen() == null ? 0 : account.getBalanceFen();
    int applied = Math.min(balance, payableFen);
    if (applied <= 0) {
      return 0;
    }
    applyDelta(userId, storeId, -applied, "SPEND", "ORDER", orderId, null, "预约抵扣");
    return applied;
  }

  @Override
  @Transactional
  public void refund(long orderId, long userId, long storeId) {
    List<BalanceLedger> rows = ledgerMapper.selectList(new LambdaQueryWrapper<BalanceLedger>()
        .eq(BalanceLedger::getStoreId, storeId)
        .eq(BalanceLedger::getUserId, userId)
        .eq(BalanceLedger::getRefType, "ORDER")
        .eq(BalanceLedger::getRefId, orderId));
    boolean refunded = rows.stream().anyMatch(row -> "REFUND".equals(row.getReason()));
    if (refunded) {
      return;
    }
    int spent = 0;
    for (BalanceLedger row : rows) {
      if ("SPEND".equals(row.getReason()) && row.getDeltaFen() != null) {
        spent += -row.getDeltaFen();
      }
    }
    if (spent <= 0) {
      return;
    }
    applyDelta(userId, storeId, spent, "REFUND", "ORDER", orderId, null, "取消退回");
  }

  private int applyDelta(
      long userId,
      long storeId,
      int delta,
      String reason,
      String refType,
      Long refId,
      Long operatorId,
      String remark) {
    BalanceAccount account = findAccount(userId, storeId);
    LocalDateTime now = LocalDateTime.now();
    int balance = account == null || account.getBalanceFen() == null ? 0 : account.getBalanceFen();
    int next = balance + delta;
    if (next < 0) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "余额不足");
    }
    if (account == null) {
      account = new BalanceAccount();
      account.setUserId(userId);
      account.setStoreId(storeId);
      account.setBalanceFen(next);
      account.setUpdatedAt(now);
      accountMapper.insert(account);
    } else {
      int updated = accountMapper.update(
          null,
          new LambdaUpdateWrapper<BalanceAccount>()
              .eq(BalanceAccount::getUserId, userId)
              .eq(BalanceAccount::getStoreId, storeId)
              .eq(BalanceAccount::getBalanceFen, balance)
              .set(BalanceAccount::getBalanceFen, next)
              .set(BalanceAccount::getUpdatedAt, now));
      if (updated == 0) {
        throw new BizException(ErrorCodes.BAD_REQUEST, "余额已变化，请重试");
      }
    }
    BalanceLedger ledger = new BalanceLedger();
    ledger.setStoreId(storeId);
    ledger.setUserId(userId);
    ledger.setDeltaFen(delta);
    ledger.setBalanceAfterFen(next);
    ledger.setReason(reason);
    ledger.setRefType(refType);
    ledger.setRefId(refId);
    ledger.setOperatorUserId(operatorId);
    ledger.setRemark(remark);
    ledger.setCreatedAt(now);
    ledgerMapper.insert(ledger);
    return next;
  }

  private BalanceAccount findAccount(long userId, long storeId) {
    return accountMapper.selectOne(new LambdaQueryWrapper<BalanceAccount>()
        .eq(BalanceAccount::getUserId, userId)
        .eq(BalanceAccount::getStoreId, storeId));
  }

  private static int requireAmount(BalanceTopupRequest request) {
    if (request == null || request.getAmountFen() == null || request.getAmountFen() <= 0) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "请填写充值金额");
    }
    if (request.getCheckinId() == null && (request.getCode() == null || request.getCode().isBlank())) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "请扫会员码或选择正在冒泡的顾客");
    }
    if (request.getAmountFen() > MAX_FEN) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "金额过大");
    }
    return request.getAmountFen();
  }
}
