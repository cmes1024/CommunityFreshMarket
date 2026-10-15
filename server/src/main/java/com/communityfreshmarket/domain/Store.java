package com.communityfreshmarket.domain;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("store")
public class Store {

  @TableId(type = IdType.AUTO)
  private Long id;
  private String name;
  private String contactPhone;
  private String contactNote;
  /** 配送结算备注快捷短语，换行分隔；空=不展示 */
  @TableField(updateStrategy = FieldStrategy.ALWAYS)
  private String deliveryRemarkPhrases;
  /** 自提结算备注快捷短语，换行分隔；空=不展示 */
  @TableField(updateStrategy = FieldStrategy.ALWAYS)
  private String pickupRemarkPhrases;
  /** 配送起送金额（分）；0=不限制 */
  private Integer minDeliveryFen;
  private String heroImageUrl;
  /** JSON 数组，最多 3 张；第一张与 heroImageUrl 相同 */
  @TableField(value = "hero_image_urls", updateStrategy = FieldStrategy.ALWAYS)
  private String heroImageUrls;
  private String homeKicker;
  private String homeTitle;
  private String homeSubtitle;
  /** 首页公告；空则顾客端不展示 */
  @TableField(updateStrategy = FieldStrategy.ALWAYS)
  private String homeNotice;
  private Integer homeProductCols;
  private String productDetailMode;
  /** 顾客会员码入口：PROFILE 我的 | TAB 底栏正中 */
  private String memberCodeEntry;
  private String status;
  /** 店员邀请码；不对外公开店铺摘要接口 */
  private String inviteCode;
  /** 与 inviteCode 对应的小程序码 PNG base64 缓存 */
  private String inviteQrcodeBase64;
  /** 每消费 1 元入账积分，最多两位小数；0=关闭 */
  private BigDecimal pointsPerYuan;
  /** 图像选品会话有效分钟数 */
  private Integer pickImageTtlMinutes;
  /** PRODUCE 青绿（默认）/ BREAKFAST 赭石 / EATERY 绛红 / MARKET 海蓝 / CUSTOM */
  private String themePreset;
  /** 自定义主色 #rrggbb；非 CUSTOM 为空 */
  @TableField(updateStrategy = FieldStrategy.ALWAYS)
  private String themePrimary;
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

  public String getDeliveryRemarkPhrases() {
    return deliveryRemarkPhrases;
  }

  public void setDeliveryRemarkPhrases(String deliveryRemarkPhrases) {
    this.deliveryRemarkPhrases = deliveryRemarkPhrases;
  }

  public String getPickupRemarkPhrases() {
    return pickupRemarkPhrases;
  }

  public void setPickupRemarkPhrases(String pickupRemarkPhrases) {
    this.pickupRemarkPhrases = pickupRemarkPhrases;
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

  public String getHeroImageUrls() {
    return heroImageUrls;
  }

  public void setHeroImageUrls(String heroImageUrls) {
    this.heroImageUrls = heroImageUrls;
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

  public String getHomeNotice() {
    return homeNotice;
  }

  public void setHomeNotice(String homeNotice) {
    this.homeNotice = homeNotice;
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

  public String getMemberCodeEntry() {
    return memberCodeEntry;
  }

  public void setMemberCodeEntry(String memberCodeEntry) {
    this.memberCodeEntry = memberCodeEntry;
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

  public BigDecimal getPointsPerYuan() {
    return pointsPerYuan;
  }

  public void setPointsPerYuan(BigDecimal pointsPerYuan) {
    this.pointsPerYuan = pointsPerYuan;
  }

  public Integer getPickImageTtlMinutes() {
    return pickImageTtlMinutes;
  }

  public void setPickImageTtlMinutes(Integer pickImageTtlMinutes) {
    this.pickImageTtlMinutes = pickImageTtlMinutes;
  }

  public String getThemePreset() {
    return themePreset;
  }

  public void setThemePreset(String themePreset) {
    this.themePreset = themePreset;
  }

  public String getThemePrimary() {
    return themePrimary;
  }

  public void setThemePrimary(String themePrimary) {
    this.themePrimary = themePrimary;
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
