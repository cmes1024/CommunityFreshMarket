package com.communityfreshmarket.points.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class PointRedeemRequest {

  @NotBlank
  @Size(max = 128)
  private String title;

  @NotNull
  @Min(1)
  private Integer costPoints;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Integer getCostPoints() {
    return costPoints;
  }

  public void setCostPoints(Integer costPoints) {
    this.costPoints = costPoints;
  }
}
