package com.communityfreshmarket.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.communityfreshmarket.auth.dto.LoginResponse;
import com.communityfreshmarket.auth.dto.UserProfileResponse;
import com.communityfreshmarket.auth.dto.WechatLoginRequest;
import com.communityfreshmarket.common.BizException;
import com.communityfreshmarket.common.ErrorCodes;
import com.communityfreshmarket.auth.dto.ProfileUpdateRequest;
import com.communityfreshmarket.config.AppProperties;
import com.communityfreshmarket.domain.StoreMember;
import com.communityfreshmarket.domain.User;
import com.communityfreshmarket.mapper.StoreMemberMapper;
import com.communityfreshmarket.mapper.UserMapper;
import com.communityfreshmarket.upload.UploadCleanup;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

  private static final Logger log = LoggerFactory.getLogger(AuthService.class);

  private final AppProperties appProperties;
  private final WechatClient wechatClient;
  private final SessionService sessionService;
  private final UserMapper userMapper;
  private final StoreMemberMapper storeMemberMapper;
  private final UploadCleanup uploadCleanup;
  private final TransactionTemplate transactionTemplate;

  public AuthService(
      AppProperties appProperties,
      WechatClient wechatClient,
      SessionService sessionService,
      UserMapper userMapper,
      StoreMemberMapper storeMemberMapper,
      UploadCleanup uploadCleanup,
      PlatformTransactionManager transactionManager) {
    this.appProperties = appProperties;
    this.wechatClient = wechatClient;
    this.sessionService = sessionService;
    this.userMapper = userMapper;
    this.storeMemberMapper = storeMemberMapper;
    this.uploadCleanup = uploadCleanup;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

  public LoginResponse loginByWechat(WechatLoginRequest request) {
    String openid = wechatClient.codeToOpenid(request.getCode());
    return transactionTemplate.execute(status -> persistLogin(openid, request));
  }

  private LoginResponse persistLogin(String openid, WechatLoginRequest request) {
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

    String token = sessionService.issue(user.getId());
    return new LoginResponse(token, sessionService.expireSeconds(), toProfile(user));
  }

  @org.springframework.transaction.annotation.Transactional
  public UserProfileResponse updateProfile(ProfileUpdateRequest request) {
    long userId = UserContext.requireUserId();
    User user = userMapper.selectById(userId);
    if (user == null) {
      throw new BizException(ErrorCodes.UNAUTHORIZED, "用户不存在");
    }
    boolean changed = false;
    if (request != null && request.getNickname() != null) {
      String nick = request.getNickname().trim();
      if (nick.isEmpty()) {
        throw new BizException(ErrorCodes.BAD_REQUEST, "请填写昵称");
      }
      if (nick.length() > 64) {
        throw new BizException(ErrorCodes.BAD_REQUEST, "昵称过长");
      }
      user.setNickname(nick);
      changed = true;
    }
    if (request != null && request.getAvatarUrl() != null) {
      String url = request.getAvatarUrl().trim();
      if (!isOwnUpload(url)) {
        throw new BizException(ErrorCodes.BAD_REQUEST, "头像无效");
      }
      String previous = user.getAvatarUrl();
      user.setAvatarUrl(url);
      changed = true;
      if (StringUtils.hasText(previous) && !previous.equals(url)) {
        uploadCleanup.release(List.of(previous));
      }
    }
    if (!changed) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "没有要保存的内容");
    }
    user.setUpdatedAt(LocalDateTime.now());
    userMapper.updateById(user);
    return toProfile(user);
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

  private boolean isOwnUpload(String url) {
    String base = appProperties.getUploadPublicBase();
    if (!StringUtils.hasText(url) || !StringUtils.hasText(base)) {
      return false;
    }
    String prefix = base.endsWith("/") ? base : base + "/";
    if (!url.startsWith(prefix) || url.contains("..") || url.indexOf('\\') >= 0) {
      return false;
    }
    String name = url.substring(prefix.length());
    return !name.isEmpty() && name.indexOf('/') < 0;
  }

  private static String blankToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }
}
