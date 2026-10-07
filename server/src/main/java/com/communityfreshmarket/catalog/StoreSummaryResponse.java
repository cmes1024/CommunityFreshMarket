package com.communityfreshmarket.catalog;

public class StoreSummaryResponse {

  private long id;
  private String name;
  private String contactPhone;
  private String contactNote;
  private String heroImageUrl;
  private String homeKicker;
  private String homeTitle;
  private String homeSubtitle;
  private int homeProductCols = 2;
  private String productDetailMode = "SHEET";

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
}
