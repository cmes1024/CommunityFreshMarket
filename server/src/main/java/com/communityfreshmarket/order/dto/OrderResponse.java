package com.communityfreshmarket.order.dto;

import com.communityfreshmarket.domain.Order;
import com.communityfreshmarket.order.OrderStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class OrderResponse {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private long id;
  private String fulfillment;
  private String status;
  private String statusLabel;
  private boolean asap;
  private String remark;
  private Map<String, Object> addressSnapshot;
  private Long pickupSlotId;
  private String pickupSlotLabel;
  private int totalAmountFen;
  private boolean merchantUnread;
  private LocalDateTime customerArrivedAt;
  private String cancelBy;
  private String cancelReason;
  private LocalDateTime cancelledAt;
  private LocalDateTime createdAt;
  private List<OrderItemResponse> items;
  private String nextStatus;
  private boolean canCancel;
  private boolean canArrive;
  private boolean canReorder;

  public static OrderResponse from(Order order, List<OrderItemResponse> items) {
    OrderStatus st = OrderStatus.require(order.getStatus());
    OrderResponse r = new OrderResponse();
    r.id = order.getId();
    r.fulfillment = order.getFulfillment();
    r.status = order.getStatus();
    r.statusLabel = st.label(order.getFulfillment());
    r.asap = Boolean.TRUE.equals(order.getAsap());
    r.remark = order.getRemark();
    r.addressSnapshot = parseAddress(order.getAddressSnapshot());
    r.pickupSlotId = order.getPickupSlotId();
    r.pickupSlotLabel = order.getPickupSlotLabel();
    r.totalAmountFen = order.getTotalAmountFen() == null ? 0 : order.getTotalAmountFen();
    r.merchantUnread = Boolean.TRUE.equals(order.getMerchantUnread());
    r.customerArrivedAt = order.getCustomerArrivedAt();
    r.cancelBy = order.getCancelBy();
    r.cancelReason = order.getCancelReason();
    r.cancelledAt = order.getCancelledAt();
    r.createdAt = order.getCreatedAt();
    r.items = items;
    r.canCancel = st.unfinished();
    r.canArrive = st == OrderStatus.READY_PICKUP && order.getCustomerArrivedAt() == null;
    r.canReorder = st == OrderStatus.COMPLETED;
    if (st.unfinished() && st != OrderStatus.CANCELLED) {
      try {
        r.nextStatus = OrderStatus.next(st, order.getFulfillment()).name();
      } catch (Exception ignored) {
        r.nextStatus = null;
      }
    }
    return r;
  }

  private static Map<String, Object> parseAddress(String json) {
    if (json == null || json.isBlank()) {
      return null;
    }
    try {
      return MAPPER.readValue(json, new TypeReference<>() {});
    } catch (Exception ex) {
      return null;
    }
  }

  public long getId() {
    return id;
  }

  public String getFulfillment() {
    return fulfillment;
  }

  public String getStatus() {
    return status;
  }

  public String getStatusLabel() {
    return statusLabel;
  }

  public boolean isAsap() {
    return asap;
  }

  public String getRemark() {
    return remark;
  }

  public Map<String, Object> getAddressSnapshot() {
    return addressSnapshot;
  }

  public Long getPickupSlotId() {
    return pickupSlotId;
  }

  public String getPickupSlotLabel() {
    return pickupSlotLabel;
  }

  public int getTotalAmountFen() {
    return totalAmountFen;
  }

  public boolean isMerchantUnread() {
    return merchantUnread;
  }

  public LocalDateTime getCustomerArrivedAt() {
    return customerArrivedAt;
  }

  public String getCancelBy() {
    return cancelBy;
  }

  public String getCancelReason() {
    return cancelReason;
  }

  public LocalDateTime getCancelledAt() {
    return cancelledAt;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public List<OrderItemResponse> getItems() {
    return items;
  }

  public String getNextStatus() {
    return nextStatus;
  }

  public boolean isCanCancel() {
    return canCancel;
  }

  public boolean isCanArrive() {
    return canArrive;
  }

  public boolean isCanReorder() {
    return canReorder;
  }
}
