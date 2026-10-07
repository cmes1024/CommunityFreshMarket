package com.communityfreshmarket.catalog.dto;

import com.communityfreshmarket.catalog.ProductImageUrls;
import com.communityfreshmarket.catalog.ProductUnit;
import com.communityfreshmarket.domain.Product;
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
}
