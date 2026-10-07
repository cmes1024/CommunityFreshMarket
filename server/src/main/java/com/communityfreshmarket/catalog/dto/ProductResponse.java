package com.communityfreshmarket.catalog.dto;

import com.communityfreshmarket.catalog.ProductUnit;
import com.communityfreshmarket.domain.Product;

public class ProductResponse {

  private long id;
  private long categoryId;
  private String name;
  private String imageUrl;
  private String unit;
  private String unitLabel;
  private String specNote;
  private int priceFen;
  private boolean inStock;
  private boolean onShelf;

  public static ProductResponse from(Product product) {
    ProductResponse r = new ProductResponse();
    r.id = product.getId();
    r.categoryId = product.getCategoryId();
    r.name = product.getName();
    r.imageUrl = product.getImageUrl();
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
}
