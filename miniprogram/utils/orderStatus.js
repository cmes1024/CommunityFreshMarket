const LABELS = {
  PENDING_CONFIRM: "待确认",
  PREPARING: "备货中",
  DELIVERING: "配送中",
  READY_PICKUP: "待自提",
  COMPLETED: "已完成",
  CANCELLED: "已取消",
};

const STEPS_DELIVERY = ["PENDING_CONFIRM", "PREPARING", "DELIVERING", "COMPLETED"];
const STEPS_PICKUP = ["PENDING_CONFIRM", "PREPARING", "READY_PICKUP", "COMPLETED"];

function label(status) {
  return LABELS[status] || status;
}

function steps(fulfillment) {
  return fulfillment === "PICKUP" ? STEPS_PICKUP : STEPS_DELIVERY;
}

/** 商家推进主按钮文案（与原型「接单备货 / 完成订单」一致） */
function advanceLabel(status, fulfillment, opts) {
  const arrived = opts && opts.customerArrived;
  if (status === "PENDING_CONFIRM") return "接单备货";
  if (status === "PREPARING") {
    return fulfillment === "PICKUP" ? "推进：待自提" : "推进：开始配送";
  }
  if (status === "DELIVERING") return "完成配送";
  if (status === "READY_PICKUP") return arrived ? "完成订单" : "完成取货";
  return "推进到下一步";
}

function itemsLine(items) {
  return (items || [])
    .map((it) => `${it.productName} ×${it.qty}${it.unitLabel || ""}`)
    .join("、");
}

function fulfillLine(order) {
  if (!order) return "";
  if (order.fulfillment === "PICKUP") {
    return `自提 · ${order.pickupSlotLabel || "时段待定"}`;
  }
  const addr = order.addressSnapshot || {};
  const place = [addr.community, addr.building, addr.room].filter(Boolean).join(" ");
  return `配送 · 尽快${place ? ` · ${place}` : ""}`;
}

function contactLine(order) {
  const addr = (order && order.addressSnapshot) || {};
  return addr.phone || "";
}

module.exports = {
  LABELS,
  label,
  steps,
  advanceLabel,
  itemsLine,
  fulfillLine,
  contactLine,
};
