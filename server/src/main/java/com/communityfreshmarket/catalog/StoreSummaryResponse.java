package com.communityfreshmarket.catalog;

import java.math.BigDecimal;
import java.util.List;

public class StoreSummaryResponse {

  private long id;
  private String name;
  private String contactPhone;
  private String contactNote;
  /** 配送结算备注快捷短语；空列表=不展示 */
  private List<String> deliveryRemarkPhrases = List.of();
  /** 自提结算备注快捷短语；空列表=不展示 */
  private List<String> pickupRemarkPhrases = List.of();
  /** 配送起送金额（分）；0=不限制 */
  private int minDeliveryFen;
  private String heroImageUrl;
  private List<String> heroImageUrls = List.of();
  private String homeKicker;
  private String homeTitle;
  private String homeSubtitle;
  private String homeNotice;
  private int homeProductCols = 2;
  private String productDetailMode = "SHEET";
  /** PROFILE 我的（默认）| TAB 底栏正中 */
  private String memberCodeEntry = "PROFILE";
  private BigDecimal pointsPerYuan = BigDecimal.ONE;
  private int pickImageTtlMinutes = 15;

  public long getId() {
    return id;
  }

  public void setId(long id) {
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

  public List<String> getDeliveryRemarkPhrases() {
    return deliveryRemarkPhrases;
  }

  public void setDeliveryRemarkPhrases(List<String> deliveryRemarkPhrases) {
    this.deliveryRemarkPhrases = deliveryRemarkPhrases == null ? List.of() : deliveryRemarkPhrases;
  }

  public List<String> getPickupRemarkPhrases() {
    return pickupRemarkPhrases;
  }

  public void setPickupRemarkPhrases(List<String> pickupRemarkPhrases) {
    this.pickupRemarkPhrases = pickupRemarkPhrases == null ? List.of() : pickupRemarkPhrases;
  }

  public int getMinDeliveryFen() {
    return minDeliveryFen;
  }

  public void setMinDeliveryFen(int minDeliveryFen) {
    this.minDeliveryFen = minDeliveryFen;
  }

  public String getHeroImageUrl() {
    return heroImageUrl;
  }

  public void setHeroImageUrl(String heroImageUrl) {
    this.heroImageUrl = heroImageUrl;
  }

  public List<String> getHeroImageUrls() {
    return heroImageUrls;
  }

  public void setHeroImageUrls(List<String> heroImageUrls) {
    this.heroImageUrls = heroImageUrls == null ? List.of() : heroImageUrls;
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

  public int getHomeProductCols() {
    return homeProductCols;
  }

  public void setHomeProductCols(int homeProductCols) {
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

  public int getPickImageTtlMinutes() {
    return pickImageTtlMinutes;
  }

  public void setPickImageTtlMinutes(int pickImageTtlMinutes) {
    this.pickImageTtlMinutes = pickImageTtlMinutes;
  }
}
