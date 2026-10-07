package com.communityfreshmarket.address.dto;

import com.communityfreshmarket.domain.UserAddress;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AddressResponse {

  private long id;
  private String phone;
  private String community;
  private String building;
  private String room;
  private boolean defaultAddress;

  public static AddressResponse from(UserAddress address) {
    AddressResponse r = new AddressResponse();
    r.id = address.getId();
    r.phone = address.getPhone();
    r.community = address.getCommunity();
    r.building = address.getBuilding();
    r.room = address.getRoom();
    r.defaultAddress = Boolean.TRUE.equals(address.getIsDefault());
    return r;
  }

  public long getId() {
    return id;
  }

  public String getPhone() {
    return phone;
  }

  public String getCommunity() {
    return community;
  }

  public String getBuilding() {
    return building;
  }

  public String getRoom() {
    return room;
  }

  @JsonProperty("isDefault")
  public boolean isDefault() {
    return defaultAddress;
  }
}
