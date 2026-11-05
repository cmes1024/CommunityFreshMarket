const api = require("../../../utils/api");
const { fenToYuan } = require("../../../utils/money");

function fmtTime(iso) {
  if (!iso) return "";
  const s = String(iso).replace("T", " ").replace(/\.\d+$/, "");
  return s.length > 16 ? s.slice(0, 16) : s;
}

function timeText(order) {
  if (order.fulfillment === "PICKUP" && order.pickupSlotLabel) {
    return fmtTime(order.pickupSlotLabel);
  }
  return fmtTime(order.createdAt);
}

Page({
  data: { list: [], loading: true, loadError: false },
  async onShow() {
    await this.reload();
  },
  async reload() {
    this.setData({ loading: true, loadError: false });
    try {
      const list = await api.orders();
      this.setData({
        list: (list || []).map((o) => ({
          ...o,
          totalYuan: fenToYuan(o.totalAmountFen),
          timeText: timeText(o),
        })),
        loading: false,
      });
    } catch (e) {
      this.setData({ loading: false, loadError: true });
    }
  },
  goDetail(e) {
    wx.navigateTo({
      url: `/packageCustomer/pages/order-detail/order-detail?id=${e.currentTarget.dataset.id}`,
    });
  },
});
