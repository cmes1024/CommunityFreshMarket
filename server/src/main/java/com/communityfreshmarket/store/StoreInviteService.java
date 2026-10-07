package com.communityfreshmarket.store;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.communityfreshmarket.auth.AuthService;
import com.communityfreshmarket.auth.UserContext;
import com.communityfreshmarket.auth.dto.UserProfileResponse;
import com.communityfreshmarket.common.BizException;
import com.communityfreshmarket.common.ErrorCodes;
import com.communityfreshmarket.config.AppProperties;
import com.communityfreshmarket.domain.Store;
import com.communityfreshmarket.domain.StoreMember;
import com.communityfreshmarket.mapper.StoreMapper;
import com.communityfreshmarket.mapper.StoreMemberMapper;
import com.communityfreshmarket.store.dto.InviteCodeResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class StoreInviteService {

  private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  private static final int CODE_LEN = 6;
  private static final SecureRandom RANDOM = new SecureRandom();

  private final StoreMapper storeMapper;
  private final StoreMemberMapper storeMemberMapper;
  private final AuthService authService;
  private final AppProperties appProperties;
  private final LinkQrCodeService linkQrCodeService;

  public StoreInviteService(
      StoreMapper storeMapper,
      StoreMemberMapper storeMemberMapper,
      AuthService authService,
      AppProperties appProperties,
      LinkQrCodeService linkQrCodeService) {
    this.storeMapper = storeMapper;
    this.storeMemberMapper = storeMemberMapper;
    this.authService = authService;
    this.appProperties = appProperties;
    this.linkQrCodeService = linkQrCodeService;
  }

  @Transactional
  public InviteCodeResponse currentOrCreate() {
    long storeId = UserContext.storeId();
    Store store = requireStore(storeId);
    if (!StringUtils.hasText(store.getInviteCode())) {
      store.setInviteCode(allocateUniqueCode());
      store.setInviteQrcodeBase64(null);
      store.setUpdatedAt(LocalDateTime.now());
      storeMapper.updateById(store);
    }
    return toResponse(store.getInviteCode());
  }

  @Transactional
  public InviteCodeResponse rotate() {
    long storeId = UserContext.storeId();
    Store store = requireStore(storeId);
    store.setInviteCode(allocateUniqueCode());
    store.setInviteQrcodeBase64(null);
    store.setUpdatedAt(LocalDateTime.now());
    storeMapper.updateById(store);
    return toResponse(store.getInviteCode());
  }

  @Transactional
  public UserProfileResponse accept(String rawCode) {
    String code = rawCode == null ? "" : rawCode.trim().toUpperCase();
    if (!StringUtils.hasText(code)) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "请输入邀请码");
    }
    Store store = storeMapper.selectOne(
        new LambdaQueryWrapper<Store>().eq(Store::getInviteCode, code));
    if (store == null) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "邀请码无效");
    }
    long userId = UserContext.requireUserId();
    StoreMember existing = storeMemberMapper.selectOne(
        new LambdaQueryWrapper<StoreMember>()
            .eq(StoreMember::getStoreId, store.getId())
            .eq(StoreMember::getUserId, userId));
    if (existing == null) {
      LocalDateTime now = LocalDateTime.now();
      StoreMember member = new StoreMember();
      member.setStoreId(store.getId());
      member.setUserId(userId);
      member.setRole("STAFF");
      member.setCreatedAt(now);
      member.setUpdatedAt(now);
      storeMemberMapper.insert(member);
    }
    return authService.currentProfile();
  }

  private InviteCodeResponse toResponse(String code) {
    String url = buildInviteUrl(code);
    String b64 = linkQrCodeService.pngBase64(url);
    return new InviteCodeResponse(code, url, b64);
  }

  /**
   * 普通链接：{INVITE_LINK_BASE}?code={邀请码}
   * 公众平台「扫普通链接二维码打开小程序」配置该 BASE 前缀即可。
   */
  String buildInviteUrl(String code) {
    String base = appProperties.getInviteLinkBase();
    if (!StringUtils.hasText(base)) {
      throw new BizException(
          ErrorCodes.BAD_REQUEST,
          "未配置邀请链接 INVITE_LINK_BASE，请先在环境变量中填写");
    }
    String trimmed = base.trim();
    String sep = trimmed.contains("?") ? "&" : "?";
    return trimmed + sep + "code=" + URLEncoder.encode(code, StandardCharsets.UTF_8);
  }

  private Store requireStore(long storeId) {
    Store store = storeMapper.selectById(storeId);
    if (store == null) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "店铺不存在");
    }
    return store;
  }

  private String allocateUniqueCode() {
    for (int i = 0; i < 20; i++) {
      String code = randomCode();
      Long count = storeMapper.selectCount(
          new LambdaQueryWrapper<Store>().eq(Store::getInviteCode, code));
      if (count == null || count == 0) {
        return code;
      }
    }
    throw new BizException(ErrorCodes.BAD_REQUEST, "生成邀请码失败，请重试");
  }

  private static String randomCode() {
    StringBuilder sb = new StringBuilder(CODE_LEN);
    for (int i = 0; i < CODE_LEN; i++) {
      sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
    }
    return sb.toString();
  }
}
