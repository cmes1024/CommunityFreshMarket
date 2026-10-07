const api = require("../../../utils/api");
const { fenToYuan } = require("../../../utils/money");
const { label, steps } = require("../../../utils/orderStatus");
const { syncCartCount } = require("../../../utils/cartSync");

Page({
  data: { order: {}, stepList: [] },
  async onLoad(query) {
    this.id = Number(query.id);
    await this.refresh();
  },
  async refresh() {
    const order = await api.order(this.id);
    const codes = steps(order.fulfillment);
    const idx = codes.indexOf(order.status);
    this.setData({
      order: { ...order, totalYuan: fenToYuan(order.totalAmountFen) },
      stepList: codes.map((code, i) => ({
        code,
        label: label(code),
        done: order.status === "CANCELLED" ? false : i <= idx,
      })),
    });
  },
  async cancel() {
    await api.cancelOrder(this.id, "顾客取消");
    await this.refresh();
  },
  async arrive() {
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
