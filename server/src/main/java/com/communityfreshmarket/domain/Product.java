package com.communityfreshmarket.domain;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
  private Integer stockQty;
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

  public Integer getStockQty() {
    return stockQty;
  }

  public void setStockQty(Integer stockQty) {
    this.stockQty = stockQty;
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
