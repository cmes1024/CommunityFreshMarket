package com.communityfreshmarket.store.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class StoreUpdateRequest {

  @Size(max = 64)
  private String name;

  @Size(max = 32)
  private String contactPhone;

  @Size(max = 1000)
  private String contactNote;

  /** 配送起送金额（分）；0=不限制 */
  @Min(0)
  @Max(10_000_000)
  private Integer minDeliveryFen;

  /** 传空字符串可清除首页头图 */
  @Size(max = 512)
  private String heroImageUrl;

  /** 传空字符串可清除，顾客端回落默认文案 */
  @Size(max = 64)
  private String homeKicker;

  /** 头图主标题；传空清除，空则顾客端不展示（避免与导航店名重复） */
  @Size(max = 64)
  private String homeTitle;

  /** 传空字符串可清除，顾客端回落默认文案 */
  @Size(max = 128)
  private String homeSubtitle;

  /** 首页商品列数：1 通栏 / 2 / 3 */
  @Min(1)
  @Max(3)
  private Integer homeProductCols;

  /** 点商品：SHEET 半屏（默认）/ PAGE 整页 */
  @Size(max = 16)
  private String productDetailMode;

  /** 会员码入口：PROFILE 我的（默认）/ TAB 底栏正中 */
  @Size(max = 16)
  private String memberCodeEntry;

  @DecimalMin("0")
  @DecimalMax("100")
  @Digits(integer = 3, fraction = 2)
  private BigDecimal pointsPerYuan;

  @Min(1)
  @Max(120)
  private Integer pickImageTtlMinutes;

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

  public String getMemberCodeEntry() {
    return memberCodeEntry;
  }

  public void setMemberCodeEntry(String memberCodeEntry) {
    this.memberCodeEntry = memberCodeEntry;
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
}
