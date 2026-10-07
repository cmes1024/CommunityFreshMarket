package com.communityfreshmarket.order.dto;

import com.communityfreshmarket.domain.PickupSlot;

public class PickupSlotResponse {

  private long id;
  private String label;
  private int sortOrder;
  private boolean enabled;

  public static PickupSlotResponse from(PickupSlot slot) {
    PickupSlotResponse r = new PickupSlotResponse();
    r.id = slot.getId();
    r.label = slot.getLabel();
    r.sortOrder = slot.getSortOrder() == null ? 0 : slot.getSortOrder();
    r.enabled = Boolean.TRUE.equals(slot.getEnabled());
    return r;
  }

  public long getId() {
    return id;
  }

  public String getLabel() {
    return label;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public boolean isEnabled() {
    return enabled;
  }
}
