const api = require("../../../utils/api");
const { fenToYuan } = require("../../../utils/money");
const {
  advanceLabel,
  itemsLine,
  fulfillLine,
} = require("../../../utils/orderStatus");

function mapOrder(o) {
  const arrived = !!o.customerArrivedAt;
  return {
    ...o,
    totalYuan: fenToYuan(o.totalAmountFen),
    itemsText: itemsLine(o.items),
    fulfillText: fulfillLine(o),
    primaryLabel: advanceLabel(o.status, o.fulfillment, { customerArrived: arrived }),
    chipLabel: arrived ? "顾客已到店" : o.statusLabel,
    chipWarn: o.status === "PENDING_CONFIRM",
    chipPulse: arrived,
    pulse: arrived,
  };
}

Page({
  data: {
    board: { urgent: [], others: [] },
    urgentCount: 0,
  },
  async onShow() {
    await this.reload();
  },
  async reload() {
    const board = await api.merchantOrders();
    const urgent = (board.urgent || []).map(mapOrder);
    const others = (board.others || []).map(mapOrder);
    const urgentCount = urgent.length;
    getApp().globalData.merchantUrgentCount = urgentCount;
    this.setData({
      board: { urgent, others },
      urgentCount,
    });
  },
  go(e) {
    wx.navigateTo({
      url: `/packageMerchant/pages/order-detail/order-detail?id=${e.currentTarget.dataset.id}`,
    });
  },
  async onAdvance(e) {
    const { id, status } = e.currentTarget.dataset;
    try {
      await api.merchantAdvance(id, status);
      await this.reload();
    } catch (err) {}
  },
  async onCancel(e) {
    const { id } = e.currentTarget.dataset;
    try {
      await api.merchantCancelOrder(id, "商家取消");
      await this.reload();
    } catch (err) {}
  },
});
