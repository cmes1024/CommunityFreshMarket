package com.communityfreshmarket.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.communityfreshmarket.auth.dto.LoginResponse;
import com.communityfreshmarket.auth.dto.UserProfileResponse;
import com.communityfreshmarket.auth.dto.WechatLoginRequest;
import com.communityfreshmarket.common.BizException;
import com.communityfreshmarket.common.ErrorCodes;
import com.communityfreshmarket.config.AppProperties;
import com.communityfreshmarket.domain.StoreMember;
import com.communityfreshmarket.domain.User;
import com.communityfreshmarket.mapper.StoreMemberMapper;
import com.communityfreshmarket.mapper.UserMapper;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

  private static final Logger log = LoggerFactory.getLogger(AuthService.class);

  private final AppProperties appProperties;
  private final WechatClient wechatClient;
  private final JwtService jwtService;
  private final UserMapper userMapper;
  private final StoreMemberMapper storeMemberMapper;

  public AuthService(
      AppProperties appProperties,
      WechatClient wechatClient,
      JwtService jwtService,
      UserMapper userMapper,
      StoreMemberMapper storeMemberMapper) {
    this.appProperties = appProperties;
    this.wechatClient = wechatClient;
    this.jwtService = jwtService;
    this.userMapper = userMapper;
    this.storeMemberMapper = storeMemberMapper;
  }

  @Transactional
  public LoginResponse loginByWechat(WechatLoginRequest request) {
    String openid = wechatClient.codeToOpenid(request.getCode());
    User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getOpenid, openid));
    LocalDateTime now = LocalDateTime.now();
    if (user == null) {
      user = new User();
      user.setOpenid(openid);
      user.setNickname(blankToNull(request.getNickname()));
      user.setAvatarUrl(blankToNull(request.getAvatarUrl()));
      user.setCreatedAt(now);
      user.setUpdatedAt(now);
      userMapper.insert(user);
    } else if (StringUtils.hasText(request.getNickname()) || StringUtils.hasText(request.getAvatarUrl())) {
      if (StringUtils.hasText(request.getNickname())) {
        user.setNickname(request.getNickname().trim());
      }
      if (StringUtils.hasText(request.getAvatarUrl())) {
        user.setAvatarUrl(request.getAvatarUrl().trim());
      }
      user.setUpdatedAt(now);
      userMapper.updateById(user);
    }

    ensureBootstrapOwner(user);

    String token = jwtService.issue(user.getId());
    return new LoginResponse(token, jwtService.expireSeconds(), toProfile(user));
  }

  public UserProfileResponse currentProfile() {
    long userId = UserContext.requireUserId();
    User user = userMapper.selectById(userId);
    if (user == null) {
      throw new BizException(ErrorCodes.UNAUTHORIZED, "用户不存在");
    }
    return toProfile(user);
  }

  public StoreMember requireMembership(long userId, long storeId) {
    StoreMember member = storeMemberMapper.selectOne(
        new LambdaQueryWrapper<StoreMember>()
            .eq(StoreMember::getStoreId, storeId)
            .eq(StoreMember::getUserId, userId));
    if (member == null) {
      throw new BizException(ErrorCodes.FORBIDDEN, "无店铺权限");
    }
    return member;
  }

  /**
   * 生产与本地同一路径：openid 命中 BOOTSTRAP_OWNER_OPENID 时，确保其为默认店唯一 OWNER。
   * 若库里已有其他人是店主（例如早期测试账号），会降为 STAFF，以部署配置为准。
   */
  private void ensureBootstrapOwner(User user) {
    String bootstrap = appProperties.getBootstrapOwnerOpenid();
    if (!StringUtils.hasText(bootstrap) || !bootstrap.trim().equals(user.getOpenid())) {
      return;
    }
    long storeId = appProperties.getDefaultStoreId();
    LocalDateTime now = LocalDateTime.now();

    StoreMember existingOwner = storeMemberMapper.selectOne(
        new LambdaQueryWrapper<StoreMember>()
            .eq(StoreMember::getStoreId, storeId)
            .eq(StoreMember::getRole, "OWNER")
            .last("LIMIT 1"));
    if (existingOwner != null && !existingOwner.getUserId().equals(user.getId())) {
      existingOwner.setRole("STAFF");
      existingOwner.setUpdatedAt(now);
      storeMemberMapper.updateById(existingOwner);
      log.warn(
          "bootstrap owner reassigned: store {} previous owner userId={} -> STAFF",
          storeId,
          existingOwner.getUserId());
    }

    StoreMember mine = storeMemberMapper.selectOne(
        new LambdaQueryWrapper<StoreMember>()
            .eq(StoreMember::getStoreId, storeId)
            .eq(StoreMember::getUserId, user.getId()));
    if (mine == null) {
      mine = new StoreMember();
      mine.setStoreId(storeId);
      mine.setUserId(user.getId());
      mine.setRole("OWNER");
      mine.setCreatedAt(now);
      mine.setUpdatedAt(now);
      storeMemberMapper.insert(mine);
      log.info("bootstrap owner bound: storeId={} userId={}", storeId, user.getId());
    } else if (!"OWNER".equals(mine.getRole())) {
      mine.setRole("OWNER");
      mine.setUpdatedAt(now);
      storeMemberMapper.updateById(mine);
      log.info("bootstrap owner promoted: storeId={} userId={}", storeId, user.getId());
    }
  }

  private UserProfileResponse toProfile(User user) {
    long browseStoreId = appProperties.getDefaultStoreId();
    StoreMember member = storeMemberMapper.selectOne(
        new LambdaQueryWrapper<StoreMember>()
            .eq(StoreMember::getUserId, user.getId())
            .eq(StoreMember::getStoreId, browseStoreId)
            .last("LIMIT 1"));
    if (member == null) {
      member = storeMemberMapper.selectOne(
          new LambdaQueryWrapper<StoreMember>()
              .eq(StoreMember::getUserId, user.getId())
              .last("LIMIT 1"));
    }

    UserProfileResponse profile = new UserProfileResponse();
    profile.setId(user.getId());
    profile.setNickname(user.getNickname());
    profile.setAvatarUrl(user.getAvatarUrl());
    profile.setBrowseStoreId(browseStoreId);
    if (member != null) {
      profile.setStoreMember(true);
      profile.setRole(member.getRole());
      profile.setManageStoreId(member.getStoreId());
    } else {
      profile.setStoreMember(false);
      profile.setRole(null);
      profile.setManageStoreId(null);
    }
    return profile;
  }

  private static String blankToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }
}
