package com.communityfreshmarket.balance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.communityfreshmarket.checkin.CheckinService;
import com.communityfreshmarket.domain.BalanceAccount;
import com.communityfreshmarket.domain.BalanceLedger;
import com.communityfreshmarket.mapper.BalanceAccountMapper;
import com.communityfreshmarket.mapper.BalanceLedgerMapper;
import com.communityfreshmarket.mapper.UserMapper;
import com.communityfreshmarket.points.MemberCodeService;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {

  @Mock BalanceAccountMapper accountMapper;
  @Mock BalanceLedgerMapper ledgerMapper;
  @Mock UserMapper userMapper;
  @Mock MemberCodeService memberCodeService;
  @Mock CheckinService checkinService;
  @InjectMocks BalanceService balanceService;

  @BeforeEach
  void initTable() {
    MybatisConfiguration configuration = new MybatisConfiguration();
    MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
    TableInfoHelper.initTableInfo(assistant, BalanceAccount.class);
    TableInfoHelper.initTableInfo(assistant, BalanceLedger.class);
  }

  @Test
  void applyDebitsMinOfBalanceAndPayable() {
    BalanceAccount account = new BalanceAccount();
    account.setUserId(3L);
    account.setStoreId(1L);
    account.setBalanceFen(800);
    when(accountMapper.selectOne(any())).thenReturn(account);
    when(accountMapper.update(isNull(), any())).thenReturn(1);

    int applied = balanceService.apply(9L, 3L, 1L, 500, true);

    assertEquals(500, applied);
    ArgumentCaptor<BalanceLedger> ledger = ArgumentCaptor.forClass(BalanceLedger.class);
    verify(ledgerMapper).insert(ledger.capture());
    assertEquals(-500, ledger.getValue().getDeltaFen());
    assertEquals(300, ledger.getValue().getBalanceAfterFen());
    assertEquals("SPEND", ledger.getValue().getReason());
    assertEquals(9L, ledger.getValue().getRefId());
  }

  @Test
  void applySkipsWhenUnused() {
    int applied = balanceService.apply(9L, 3L, 1L, 500, false);
    assertEquals(0, applied);
    verify(accountMapper, never()).selectOne(any());
  }

  @Test
  void refundCreditsTheSpendOnce() {
    BalanceLedger spent = new BalanceLedger();
    spent.setReason("SPEND");
    spent.setDeltaFen(-500);
    when(ledgerMapper.selectList(any())).thenReturn(List.of(spent));
    BalanceAccount account = new BalanceAccount();
    account.setBalanceFen(300);
    when(accountMapper.selectOne(any())).thenReturn(account);
    when(accountMapper.update(isNull(), any())).thenReturn(1);

    balanceService.refund(9L, 3L, 1L);

    ArgumentCaptor<BalanceLedger> ledger = ArgumentCaptor.forClass(BalanceLedger.class);
    verify(ledgerMapper).insert(ledger.capture());
    assertEquals(500, ledger.getValue().getDeltaFen());
    assertEquals("REFUND", ledger.getValue().getReason());
    assertEquals(800, ledger.getValue().getBalanceAfterFen());
  }

  @Test
  void refundIsIdempotent() {
    BalanceLedger spent = new BalanceLedger();
    spent.setReason("SPEND");
    spent.setDeltaFen(-500);
    BalanceLedger refunded = new BalanceLedger();
    refunded.setReason("REFUND");
    refunded.setDeltaFen(500);
    when(ledgerMapper.selectList(any())).thenReturn(List.of(spent, refunded));

    balanceService.refund(9L, 3L, 1L);

    verify(ledgerMapper, never()).insert(any(BalanceLedger.class));
  }
}
