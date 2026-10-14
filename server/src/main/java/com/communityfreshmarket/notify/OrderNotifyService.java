package com.communityfreshmarket.notify;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.communityfreshmarket.domain.Order;
import com.communityfreshmarket.domain.StoreMember;
import com.communityfreshmarket.domain.User;
import com.communityfreshmarket.mapper.StoreMemberMapper;
import com.communityfreshmarket.mapper.UserMapper;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class OrderNotifyService {

  private static final String MERCHANT_ORDERS = "packageMerchant/pages/orders/orders";
  private static final String CUSTOMER_ORDER = "packageCustomer/pages/order-detail/order-detail?id=";

  private final SubscribeMessageService subscribeMessageService;
  private final StoreMemberMapper storeMemberMapper;
  private final UserMapper userMapper;

  public OrderNotifyService(
      SubscribeMessageService subscribeMessageService,
      StoreMemberMapper storeMemberMapper,
      UserMapper userMapper) {
    this.subscribeMessageService = subscribeMessageService;
    this.storeMemberMapper = storeMemberMapper;
    this.userMapper = userMapper;
  }

  public void notifyNewOrder(Order order) {
    String summary = orderSummary(order);
    for (String openid : memberOpenids(order.getStoreId())) {
      subscribeMessageService.sendNewOrder(openid, summary, MERCHANT_ORDERS);
    }
  }

  public void notifyCancel(Order order, String by) {
    if (!"CUSTOMER".equals(by)) {
      return;
    }
    String summary = orderSummary(order);
    for (String openid : memberOpenids(order.getStoreId())) {
      subscribeMessageService.sendCancel(openid, summary, MERCHANT_ORDERS);
    }
  }

  public void notifyDelivering(Order order) {
    String openid = userOpenid(order.getUserId());
    if (openid == null) {
      return;
    }
    subscribeMessageService.sendDelivering(
        openid, orderSummary(order), CUSTOMER_ORDER + order.getId());
  }

  private List<String> memberOpenids(long storeId) {
    List<StoreMember> members = storeMemberMapper.selectList(
        new LambdaQueryWrapper<StoreMember>().eq(StoreMember::getStoreId, storeId));
    return members.stream()
        .map(StoreMember::getUserId)
        .filter(Objects::nonNull)
        .map(this::userOpenid)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
  }

  private String userOpenid(Long userId) {
    if (userId == null) {
      return null;
    }
    User user = userMapper.selectById(userId);
    return user == null ? null : user.getOpenid();
  }

  private static String orderSummary(Order order) {
    int fen = order.getTotalAmountFen() == null ? 0 : order.getTotalAmountFen();
    String yuan = String.format("%.2f", fen / 100.0);
    return "#" + order.getId() + " ¥" + yuan;
  }
}
