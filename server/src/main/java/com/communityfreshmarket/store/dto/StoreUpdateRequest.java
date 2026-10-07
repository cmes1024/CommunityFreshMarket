package com.communityfreshmarket.store.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class StoreUpdateRequest {

  @Size(max = 64)
  private String name;

  @Size(max = 32)
  private String contactPhone;

  @Size(max = 1000)
  private String contactNote;

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
}
