package com.communityfreshmarket.livephoto.dto;

public class LivePhotoApplyResponse {

  private int applicantCount;
  private boolean requestedByMe;

  public LivePhotoApplyResponse(int applicantCount, boolean requestedByMe) {
    this.applicantCount = applicantCount;
    this.requestedByMe = requestedByMe;
  }

  public int getApplicantCount() {
    return applicantCount;
  }

  public boolean isRequestedByMe() {
    return requestedByMe;
  }
}
