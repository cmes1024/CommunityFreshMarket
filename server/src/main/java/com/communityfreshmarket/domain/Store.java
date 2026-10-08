package com.communityfreshmarket.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("store")
public class Store {

  @TableId(type = IdType.AUTO)
  private Long id;
  private String name;
  private String contactPhone;
  private String contactNote;
  /** 配送起送金额（分）；0=不限制 */
  private Integer minDeliveryFen;
  private String heroImageUrl;
  private String homeKicker;
  private String homeTitle;
  private String homeSubtitle;
  private Integer homeProductCols;
  private String productDetailMode;
  private String status;
  /** 店员邀请码；不对外公开店铺摘要接口 */
  private String inviteCode;
  /** 与 inviteCode 对应的小程序码 PNG base64 缓存 */
  private String inviteQrcodeBase64;
  /** 每消费 1 元入账积分；0=关闭 */
  private Integer pointsPerYuan;
  /** 图像选品会话有效分钟数 */
  private Integer pickImageTtlMinutes;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getContactPhone() {
    return contactPhone;
  }

  public void setContactPhone(String contactPhone) {
    this.contactPhone = contactPhone;
  }

  public String getContactNote() {
    return contactNote;
  }

  public void setContactNote(String contactNote) {
    this.contactNote = contactNote;
  }

  public Integer getMinDeliveryFen() {
    return minDeliveryFen;
  }

  public void setMinDeliveryFen(Integer minDeliveryFen) {
    this.minDeliveryFen = minDeliveryFen;
  }

  public String getHeroImageUrl() {
    return heroImageUrl;
  }

  public void setHeroImageUrl(String heroImageUrl) {
    this.heroImageUrl = heroImageUrl;
  }

  public String getHomeKicker() {
    return homeKicker;
  }

  public void setHomeKicker(String homeKicker) {
    this.homeKicker = homeKicker;
  }

  public String getHomeTitle() {
    return homeTitle;
  }

  public void setHomeTitle(String homeTitle) {
    this.homeTitle = homeTitle;
  }

  public String getHomeSubtitle() {
    return homeSubtitle;
  }

  public void setHomeSubtitle(String homeSubtitle) {
    this.homeSubtitle = homeSubtitle;
  }

  public Integer getHomeProductCols() {
    return homeProductCols;
  }

  public void setHomeProductCols(Integer homeProductCols) {
    this.homeProductCols = homeProductCols;
  }

  public String getProductDetailMode() {
    return productDetailMode;
  }

  public void setProductDetailMode(String productDetailMode) {
    this.productDetailMode = productDetailMode;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getInviteCode() {
    return inviteCode;
  }

  public void setInviteCode(String inviteCode) {
    this.inviteCode = inviteCode;
  }

  public String getInviteQrcodeBase64() {
    return inviteQrcodeBase64;
  }

  public void setInviteQrcodeBase64(String inviteQrcodeBase64) {
    this.inviteQrcodeBase64 = inviteQrcodeBase64;
  }

  public Integer getPointsPerYuan() {
    return pointsPerYuan;
  }

  public void setPointsPerYuan(Integer pointsPerYuan) {
    this.pointsPerYuan = pointsPerYuan;
  }

  public Integer getPickImageTtlMinutes() {
    return pickImageTtlMinutes;
  }

  public void setPickImageTtlMinutes(Integer pickImageTtlMinutes) {
    this.pickImageTtlMinutes = pickImageTtlMinutes;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
