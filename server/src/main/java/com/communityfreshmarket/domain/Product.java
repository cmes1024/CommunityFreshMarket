package com.communityfreshmarket.domain;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("product")
public class Product {

  @TableId(type = IdType.AUTO)
  private Long id;
  private Long storeId;
  private Long categoryId;
  private String name;
  /** 封面图 = imageUrls 第一张；可清空 */
  @TableField(updateStrategy = FieldStrategy.ALWAYS)
  private String imageUrl;
  /** JSON 数组字符串，最多 3 张；可清空 */
  @TableField(value = "image_urls", updateStrategy = FieldStrategy.ALWAYS)
  private String imageUrls;
  private String unit;
  /** 可清空：空串保存为 null 时也要写库 */
  @TableField(updateStrategy = FieldStrategy.ALWAYS)
  private String specNote;
  private Integer priceFen;
  private Boolean inStock;
  private Boolean onShelf;
  private Boolean homeRecommended;
  /** NULL = 不追踪数量 */
  @TableField(updateStrategy = FieldStrategy.ALWAYS)
  private BigDecimal stockQty;
  @TableField(updateStrategy = FieldStrategy.ALWAYS)
  private BigDecimal minQty;
  @TableField(updateStrategy = FieldStrategy.ALWAYS)
  private BigDecimal maxQty;
  private Boolean livePhotoEnabled;
  @TableField(updateStrategy = FieldStrategy.ALWAYS)
  private String livePhotoUrl;
  @TableField(updateStrategy = FieldStrategy.ALWAYS)
  private LocalDateTime livePhotoTakenAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getStoreId() {
    return storeId;
  }

  public void setStoreId(Long storeId) {
    this.storeId = storeId;
  }

  public Long getCategoryId() {
    return categoryId;
  }

  public void setCategoryId(Long categoryId) {
    this.categoryId = categoryId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  public String getImageUrls() {
    return imageUrls;
  }

  public void setImageUrls(String imageUrls) {
    this.imageUrls = imageUrls;
  }

  public String getUnit() {
    return unit;
  }

  public void setUnit(String unit) {
    this.unit = unit;
  }

  public String getSpecNote() {
    return specNote;
  }

  public void setSpecNote(String specNote) {
    this.specNote = specNote;
  }

  public Integer getPriceFen() {
    return priceFen;
  }

  public void setPriceFen(Integer priceFen) {
    this.priceFen = priceFen;
  }

  public Boolean getInStock() {
    return inStock;
  }

  public void setInStock(Boolean inStock) {
    this.inStock = inStock;
  }

  public Boolean getOnShelf() {
    return onShelf;
  }

  public void setOnShelf(Boolean onShelf) {
    this.onShelf = onShelf;
  }

  public Boolean getHomeRecommended() {
    return homeRecommended;
  }

  public void setHomeRecommended(Boolean homeRecommended) {
    this.homeRecommended = homeRecommended;
  }

  public BigDecimal getStockQty() {
    return stockQty;
  }

  public void setStockQty(BigDecimal stockQty) {
    this.stockQty = stockQty;
  }

  public BigDecimal getMinQty() {
    return minQty;
  }

  public void setMinQty(BigDecimal minQty) {
    this.minQty = minQty;
  }

  public BigDecimal getMaxQty() {
    return maxQty;
  }

  public void setMaxQty(BigDecimal maxQty) {
    this.maxQty = maxQty;
  }

  public Boolean getLivePhotoEnabled() {
    return livePhotoEnabled;
  }

  public void setLivePhotoEnabled(Boolean livePhotoEnabled) {
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
