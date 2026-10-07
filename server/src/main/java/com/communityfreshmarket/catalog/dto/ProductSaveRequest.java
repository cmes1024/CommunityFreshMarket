package com.communityfreshmarket.catalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ProductSaveRequest {

  @NotNull
  private Long categoryId;

  @NotBlank
  @Size(max = 128)
  private String name;

  @Size(max = 512)
  private String imageUrl;

  @NotBlank
  private String unit;

  @Size(max = 64)
  private String specNote;

  @NotNull
  @Min(0)
  private Integer priceFen;

  private Boolean inStock;
  private Boolean onShelf;

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
}
