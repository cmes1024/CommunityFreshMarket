package com.communityfreshmarket.livephoto.dto;

import com.communityfreshmarket.domain.LivePhotoRequest;
import java.time.LocalDateTime;

public class LivePhotoRequestItemResponse {

  private long id;
  private long productId;
  private String productName;
  private int applicantCount;
  private LocalDateTime lastRequestedAt;

  public static LivePhotoRequestItemResponse from(LivePhotoRequest request, String productName) {
    LivePhotoRequestItemResponse r = new LivePhotoRequestItemResponse();
    r.id = request.getId();
    r.productId = request.getProductId();
    r.productName = productName;
    r.applicantCount = request.getApplicantCount() == null ? 0 : request.getApplicantCount();
    r.lastRequestedAt = request.getLastRequestedAt();
    return r;
  }

  public long getId() {
    return id;
  }

  public long getProductId() {
    return productId;
  }

  public String getProductName() {
    return productName;
  }

  public int getApplicantCount() {
    return applicantCount;
  }

  public LocalDateTime getLastRequestedAt() {
    return lastRequestedAt;
  }
}
