package com.communityfreshmarket.catalog.dto;

import com.communityfreshmarket.domain.Category;

public class CategoryResponse {

  private long id;
  private String name;
  private int sortOrder;
  private boolean enabled;

  public static CategoryResponse from(Category category) {
    CategoryResponse r = new CategoryResponse();
    r.id = category.getId();
    r.name = category.getName();
    r.sortOrder = category.getSortOrder() == null ? 0 : category.getSortOrder();
    r.enabled = Boolean.TRUE.equals(category.getEnabled());
    return r;
  }

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public boolean isEnabled() {
    return enabled;
  }
}
