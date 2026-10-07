package com.communityfreshmarket.auth.dto;

public class UserProfileResponse {

  private long id;
  private String nickname;
  private String avatarUrl;
  private boolean storeMember;
  private String role;
  private long browseStoreId;
  private Long manageStoreId;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getNickname() {
    return nickname;
  }

  public void setNickname(String nickname) {
    this.nickname = nickname;
  }

  public String getAvatarUrl() {
    return avatarUrl;
  }

  public void setAvatarUrl(String avatarUrl) {
    this.avatarUrl = avatarUrl;
  }

  public boolean isStoreMember() {
    return storeMember;
  }

  public void setStoreMember(boolean storeMember) {
    this.storeMember = storeMember;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public long getBrowseStoreId() {
    return browseStoreId;
  }

  public void setBrowseStoreId(long browseStoreId) {
    this.browseStoreId = browseStoreId;
  }

  public Long getManageStoreId() {
    return manageStoreId;
  }

  public void setManageStoreId(Long manageStoreId) {
    this.manageStoreId = manageStoreId;
  }
}
