const api = require("../../../utils/api");
const copy = require("../../../utils/copy");
const { fenToYuan } = require("../../../utils/money");
const { label, steps } = require("../../../utils/orderStatus");
const { syncCartCount } = require("../../../utils/cartSync");
const { requestSubscribe } = require("../../../utils/subscribe");

function fmtTime(iso) {
  if (!iso) return "";
  const s = String(iso).replace("T", " ").replace(/\.\d+$/, "");
  return s.length > 16 ? s.slice(0, 16) : s;
}

function fulfillParts(order) {
  if (order.fulfillment === "PICKUP") {
    return {
      fulfillLabel: copy.checkout.pickup,
      fulfillText: fmtTime(order.pickupSlotLabel) || "时段待定",
    };
  }
  const addr = order.addressSnapshot || {};
  const place = [addr.community, addr.building, addr.room].filter(Boolean).join(" ");
  return {
    fulfillLabel: copy.checkout.delivery,
    fulfillText: place ? `${copy.checkout.asap} · ${place}` : copy.checkout.asap,
  };
}

Page({
  data: { order: {}, stepList: [], fulfillLabel: "", fulfillText: "" },
  async onLoad(query) {
    this.id = Number(query.id);
    await this.refresh();
  },
  async refresh() {
    const order = await api.order(this.id);
    const codes = steps(order.fulfillment);
    const idx = codes.indexOf(order.status);
    const cancelled = order.status === "CANCELLED";
    const completed = order.status === "COMPLETED";
    this.setData({
      order: {
        ...order,
        totalYuan: fenToYuan(order.totalAmountFen),
        discountYuan: fenToYuan(order.discountFen || 0),
        balanceYuan: fenToYuan(order.balanceFen || 0),
        payYuan: fenToYuan(order.payFen != null ? order.payFen : order.totalAmountFen),
        dueYuan: fenToYuan(
          order.dueFen != null
            ? order.dueFen
            : Math.max(0, (order.payFen != null ? order.payFen : order.totalAmountFen) - (order.balanceFen || 0))
        ),
      },
      stepList: codes.map((code, i) => ({
        code,
        label: label(code),
        done: !cancelled && (completed || i < idx),
        current: !cancelled && !completed && i === idx,
        reached: !cancelled && i > 0 && (completed || i <= idx),
      })),
      ...fulfillParts(order),
    });
  },
  async cancel() {
    const ok = await askCancel();
    if (!ok) return;
    try {
      await requestSubscribe(["cancelTemplateId"]);
      await api.cancelOrder(this.id, "顾客取消");
      await this.refresh();
    } catch (e) {}
  },
  async arrive() {
    await requestSubscribe(["arrivedTemplateId"]);
    await api.arriveOrder(this.id);
    await this.refresh();
  },
  async reorder() {
    const res = await api.reorder(this.id);
    await syncCartCount();
    if (res.skipped && res.skipped.length) {
      wx.showToast({ title: `部分跳过：${res.skipped[0]}`, icon: "none" });
    }
    wx.reLaunch({ url: "/packageCustomer/pages/cart/cart" });
  },
});

function askCancel() {
  return new Promise((resolve) => {
    wx.showModal({
      title: copy.order.cancelConfirmTitle,
      content: copy.order.cancelConfirm,
      confirmText: copy.order.cancelConfirmOk,
      cancelText: copy.order.cancelConfirmBack,
      confirmColor: "#DC2626",
      success: (res) => resolve(!!res.confirm),
      fail: () => resolve(false),
    });
  });
}
