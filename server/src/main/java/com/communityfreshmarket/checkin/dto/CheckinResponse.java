package com.communityfreshmarket.checkin.dto;

import com.communityfreshmarket.domain.MemberCheckin;
import java.time.LocalDateTime;

public class CheckinResponse {

  private long id;
  private long userId;
  private String nickname;
  private String avatarUrl;
  private LocalDateTime expiresAt;
  private LocalDateTime createdAt;

  public static CheckinResponse from(MemberCheckin checkin, String nickname, String avatarUrl) {
    CheckinResponse r = new CheckinResponse();
    r.id = checkin.getId();
    r.userId = checkin.getUserId();
    r.nickname = nickname;
    r.avatarUrl = avatarUrl;
    r.expiresAt = checkin.getExpiresAt();
    r.createdAt = checkin.getCreatedAt();
    return r;
  }

  public long getId() {
    return id;
  }

  public long getUserId() {
    return userId;
  }

  public String getNickname() {
    return nickname;
  }

  public String getAvatarUrl() {
    return avatarUrl;
  }

  public LocalDateTime getExpiresAt() {
    return expiresAt;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}
