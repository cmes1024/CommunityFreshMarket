package com.communityfreshmarket.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public class ProductBatchRequest {

  @NotEmpty(message = "请选择商品")
  @Size(max = 100, message = "一次最多操作 100 个商品")
  private List<Long> ids;

  @NotBlank(message = "请选择操作")
  private String op;

  public List<Long> getIds() {
    return ids;
  }

  public void setIds(List<Long> ids) {
    this.ids = ids;
  }

  public String getOp() {
    return op;
  }

  public void setOp(String op) {
    this.op = op;
  }
}
