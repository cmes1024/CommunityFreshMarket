package com.communityfreshmarket.catalog.dto;

import com.communityfreshmarket.catalog.ProductImageUrls;
import com.communityfreshmarket.catalog.ProductQtyRules;
import com.communityfreshmarket.catalog.ProductUnit;
import com.communityfreshmarket.domain.Product;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ProductResponse {

  private long id;
  private long categoryId;
  private String name;
  private String imageUrl;
  private List<String> imageUrls;
  private String unit;
  private String unitLabel;
  private String specNote;
  private int priceFen;
  private boolean inStock;
  private boolean onShelf;
  private boolean homeRecommended;
  private BigDecimal stockQty;
  private BigDecimal minQty;
  private BigDecimal maxQty;
  /** 顾客端步进器用的有效上限（限购与库存取严） */
  private BigDecimal effectiveMaxQty;
  private BigDecimal effectiveMinQty;
  private boolean livePhotoEnabled;
  private String livePhotoUrl;
  private LocalDateTime livePhotoTakenAt;
  private boolean livePhotoExpired;
  private int livePhotoApplicantCount;
  private boolean livePhotoRequestedByMe;

  public static ProductResponse from(Product product) {
    ProductResponse r = new ProductResponse();
    r.id = product.getId();
    r.categoryId = product.getCategoryId();
    r.name = product.getName();
    List<String> urls = ProductImageUrls.resolve(product.getImageUrls(), product.getImageUrl());
    r.imageUrls = urls;
    r.imageUrl = ProductImageUrls.cover(urls);
    r.unit = product.getUnit();
    try {
      r.unitLabel = ProductUnit.valueOf(product.getUnit()).getLabel();
    } catch (Exception ex) {
      r.unitLabel = product.getUnit();
    }
    r.specNote = product.getSpecNote();
    r.priceFen = product.getPriceFen() == null ? 0 : product.getPriceFen();
    r.inStock = Boolean.TRUE.equals(product.getInStock());
    r.onShelf = Boolean.TRUE.equals(product.getOnShelf());
    r.homeRecommended = Boolean.TRUE.equals(product.getHomeRecommended());
    r.stockQty = product.getStockQty();
    r.minQty = product.getMinQty();
    r.maxQty = product.getMaxQty();
    r.effectiveMaxQty = ProductQtyRules.effectiveMax(product);
    r.effectiveMinQty = ProductQtyRules.effectiveMin(product);
    r.livePhotoEnabled = Boolean.TRUE.equals(product.getLivePhotoEnabled());
    r.livePhotoUrl = product.getLivePhotoUrl();
    r.livePhotoTakenAt = product.getLivePhotoTakenAt();
    return r;
  }

  public long getId() {
    return id;
  }

  public long getCategoryId() {
    return categoryId;
  }

  public String getName() {
    return name;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public List<String> getImageUrls() {
    return imageUrls;
  }

  public String getUnit() {
    return unit;
  }

  public String getUnitLabel() {
    return unitLabel;
  }

  public String getSpecNote() {
    return specNote;
  }

  public int getPriceFen() {
    return priceFen;
  }

  public boolean isInStock() {
    return inStock;
  }

  public boolean isOnShelf() {
    return onShelf;
  }

  public boolean isHomeRecommended() {
    return homeRecommended;
  }

  public BigDecimal getStockQty() {
    return stockQty;
  }

  public BigDecimal getMinQty() {
    return minQty;
  }

  public BigDecimal getMaxQty() {
    return maxQty;
  }

  public BigDecimal getEffectiveMaxQty() {
    return effectiveMaxQty;
  }

  public BigDecimal getEffectiveMinQty() {
    return effectiveMinQty;
  }

  public boolean isLivePhotoEnabled() {
    return livePhotoEnabled;
  }

  public void setLivePhotoEnabled(boolean livePhotoEnabled) {
    this.livePhotoEnabled = livePhotoEnabled;
  }

  public String getLivePhotoUrl() {
    return livePhotoUrl;
  }

  public void setLivePhotoUrl(String livePhotoUrl) {
    this.livePhotoUrl = livePhotoUrl;
  }

  public LocalDateTime getLivePhotoTakenAt() {
    return livePhotoTakenAt;
  }

  public void setLivePhotoTakenAt(LocalDateTime livePhotoTakenAt) {
    this.livePhotoTakenAt = livePhotoTakenAt;
  }

  public boolean isLivePhotoExpired() {
    return livePhotoExpired;
  }

  public void setLivePhotoExpired(boolean livePhotoExpired) {
    this.livePhotoExpired = livePhotoExpired;
  }

  public int getLivePhotoApplicantCount() {
    return livePhotoApplicantCount;
  }

  public void setLivePhotoApplicantCount(int livePhotoApplicantCount) {
    this.livePhotoApplicantCount = livePhotoApplicantCount;
  }

  public boolean isLivePhotoRequestedByMe() {
    return livePhotoRequestedByMe;
  }

  public void setLivePhotoRequestedByMe(boolean livePhotoRequestedByMe) {
    this.livePhotoRequestedByMe = livePhotoRequestedByMe;
  }
}
