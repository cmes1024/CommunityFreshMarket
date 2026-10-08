package com.communityfreshmarket.points;

import com.communityfreshmarket.auth.UserContext;
import com.communityfreshmarket.common.BizException;
import com.communityfreshmarket.common.ErrorCodes;
import com.communityfreshmarket.domain.User;
import com.communityfreshmarket.mapper.UserMapper;
import com.communityfreshmarket.points.dto.MemberCodeResponse;
import com.communityfreshmarket.store.LinkQrCodeService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MemberCodeService {

  static final String PREFIX = "CFM-MEMBER:";

  private final UserMapper userMapper;
  private final LinkQrCodeService linkQrCodeService;

  public MemberCodeService(UserMapper userMapper, LinkQrCodeService linkQrCodeService) {
    this.userMapper = userMapper;
    this.linkQrCodeService = linkQrCodeService;
  }

  public MemberCodeResponse mine() {
    long userId = UserContext.requireUserId();
    long storeId = UserContext.storeId();
    User user = userMapper.selectById(userId);
    String code = PREFIX + storeId + ":" + userId;
    MemberCodeResponse response = new MemberCodeResponse();
    response.setCode(code);
    response.setQrcodeBase64(linkQrCodeService.pngBase64(code));
    response.setNickname(user == null ? null : user.getNickname());
    return response;
  }

  public long parseUserId(String raw, long storeId) {
    if (!StringUtils.hasText(raw) || !raw.trim().startsWith(PREFIX)) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "这不是会员码");
    }
    String rest = raw.trim().substring(PREFIX.length());
    int colon = rest.indexOf(':');
    if (colon <= 0 || colon >= rest.length() - 1) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "会员码无效");
    }
    long codeStore;
    long userId;
    try {
      codeStore = Long.parseLong(rest.substring(0, colon));
      userId = Long.parseLong(rest.substring(colon + 1));
    } catch (NumberFormatException ex) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "会员码无效");
    }
    if (codeStore != storeId) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "这不是本店会员码");
    }
    if (userId <= 0) {
      throw new BizException(ErrorCodes.BAD_REQUEST, "会员码无效");
    }
    return userId;
  }
}
