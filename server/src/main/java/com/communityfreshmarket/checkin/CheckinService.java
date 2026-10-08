package com.communityfreshmarket.checkin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.communityfreshmarket.auth.UserContext;
import com.communityfreshmarket.common.BizException;
import com.communityfreshmarket.common.ErrorCodes;
import com.communityfreshmarket.domain.MemberCheckin;
import com.communityfreshmarket.domain.User;
import com.communityfreshmarket.mapper.MemberCheckinMapper;
import com.communityfreshmarket.mapper.UserMapper;
import com.communityfreshmarket.checkin.dto.CheckinRequest;
import com.communityfreshmarket.checkin.dto.CheckinResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckinService {

  private static final int TTL_MINUTES = 2;

  private final MemberCheckinMapper checkinMapper;
  private final UserMapper userMapper;

  public CheckinService(MemberCheckinMapper checkinMapper, UserMapper userMapper) {
    this.checkinMapper = checkinMapper;
    this.userMapper = userMapper;
  }

  @Transactional
  public CheckinResponse checkin(CheckinRequest request) {
    long userId = UserContext.requireUserId();
    long storeId = UserContext.storeId();
    LocalDateTime now = LocalDateTime.now();
    // 同一用户刷新有效报到
    List<MemberCheckin> active = checkinMapper.selectList(new LambdaQueryWrapper<MemberCheckin>()
        .eq(MemberCheckin::getStoreId, storeId)
        .eq(MemberCheckin::getUserId, userId)
        .isNull(MemberCheckin::getConsumedAt)
        .gt(MemberCheckin::getExpiresAt, now));
    for (MemberCheckin c : active) {
      c.setExpiresAt(now);
      checkinMapper.updateById(c);
    }
    MemberCheckin checkin = new MemberCheckin();
    checkin.setStoreId(storeId);
    checkin.setUserId(userId);
    checkin.setExpiresAt(now.plusMinutes(TTL_MINUTES));
    checkin.setCreatedAt(now);
    if (request != null) {
      checkin.setLat(request.getLat());
      checkin.setLng(request.getLng());
    }
    checkinMapper.insert(checkin);
    return toResponse(checkin);
  }

  public List<CheckinResponse> listActive() {
    long storeId = UserContext.storeId();
    LocalDateTime now = LocalDateTime.now();
    return checkinMapper.selectList(new LambdaQueryWrapper<MemberCheckin>()
            .eq(MemberCheckin::getStoreId, storeId)
            .isNull(MemberCheckin::getConsumedAt)
            .gt(MemberCheckin::getExpiresAt, now)
            .orderByDesc(MemberCheckin::getId))
        .stream()
        .map(this::toResponse)
        .toList();
  }

  public long activeUserId(long checkinId, long storeId) {
    return requireActive(checkinId, storeId).getUserId();
  }

  @Transactional
  public long consume(long checkinId, long storeId) {
    MemberCheckin checkin = requireActive(checkinId, storeId);
    checkin.setConsumedAt(LocalDateTime.now());
    checkinMapper.updateById(checkin);
    return checkin.getUserId();
  }

  @Transactional
  public void consumeActiveForUser(long storeId, long userId) {
    LocalDateTime now = LocalDateTime.now();
    List<MemberCheckin> active = checkinMapper.selectList(new LambdaQueryWrapper<MemberCheckin>()
        .eq(MemberCheckin::getStoreId, storeId)
        .eq(MemberCheckin::getUserId, userId)
        .isNull(MemberCheckin::getConsumedAt)
        .gt(MemberCheckin::getExpiresAt, now));
    for (MemberCheckin checkin : active) {
      checkin.setConsumedAt(now);
      checkinMapper.updateById(checkin);
    }
  }

  private MemberCheckin requireActive(long checkinId, long storeId) {
    MemberCheckin checkin = checkinMapper.selectById(checkinId);
    if (checkin == null || !checkin.getStoreId().equals(storeId)) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "冒泡不存在");
    }
    if (checkin.getConsumedAt() != null) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "这次冒泡已经用过");
    }
    if (checkin.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "冒泡已过期，请让顾客再点一次");
    }
    return checkin;
  }

  private CheckinResponse toResponse(MemberCheckin checkin) {
    User user = userMapper.selectById(checkin.getUserId());
    String nick = user == null ? null : user.getNickname();
    String avatar = user == null ? null : user.getAvatarUrl();
    return CheckinResponse.from(checkin, nick, avatar);
  }
}
