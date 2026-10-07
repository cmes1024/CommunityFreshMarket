package com.communityfreshmarket.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("order_item")
public class OrderItem {

  @TableId(type = IdType.AUTO)
  private Long id;
  private Long orderId;
  private Long storeId;
  private Long productId;
  private String productName;
  private String unit;
  private Integer unitPriceFen;
  private BigDecimal qty;
  private Integer amountFen;
  private LocalDateTime createdAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getOrderId() {
    return orderId;
  }

  public void setOrderId(Long orderId) {
    this.orderId = orderId;
  }

  public Long getStoreId() {
    return storeId;
  }

  public void setStoreId(Long storeId) {
    this.storeId = storeId;
  }

  public Long getProductId() {
    return productId;
  }

  public void setProductId(Long productId) {
    this.productId = productId;
  }

  public String getProductName() {
    return productName;
  }

  public void setProductName(String productName) {
    this.productName = productName;
  }

  public String getUnit() {
    return unit;
  }

  public void setUnit(String unit) {
    this.unit = unit;
  }

  public Integer getUnitPriceFen() {
    return unitPriceFen;
  }

  public void setUnitPriceFen(Integer unitPriceFen) {
    this.unitPriceFen = unitPriceFen;
  }

  public BigDecimal getQty() {
    return qty;
  }

  public void setQty(BigDecimal qty) {
    this.qty = qty;
  }

  public Integer getAmountFen() {
    return amountFen;
  }

  public void setAmountFen(Integer amountFen) {
    this.amountFen = amountFen;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
