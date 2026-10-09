const api = require("../../../utils/api");
const copy = require("../../../utils/copy");
const { fenToYuan } = require("../../../utils/money");
const { format } = require("../../../utils/copy");
const {
  label,
  steps,
  advanceLabel,
  itemsLine,
  fulfillLine,
  contactLine,
} = require("../../../utils/orderStatus");
const { requestSubscribe } = require("../../../utils/subscribe");

function payNote(order) {
  const balanceFen = order.balanceFen || 0;
  const dueFen = order.dueFen != null
    ? order.dueFen
    : Math.max(0, (order.payFen != null ? order.payFen : order.totalAmountFen || 0) - balanceFen);
  if (balanceFen > 0 && dueFen === 0) return copy.merchant.balanceCovered;
  if (balanceFen > 0) return format(copy.merchant.balancePartial, { yuan: fenToYuan(balanceFen) });
  return copy.merchant.offlinePay;
}

Page({
  data: {
    order: {},
    stepList: [],
    advanceText: "",
    itemsText: "",
    fulfillText: "",
    contactText: "",
  },
  async onLoad(query) {
    this.id = Number(query.id);
    await this.refresh();
  },
  async refresh() {
    const order = await api.merchantOrder(this.id);
    const codes = steps(order.fulfillment);
    const idx = codes.indexOf(order.status);
    const arrived = !!order.customerArrivedAt;
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
        payNote: payNote(order),
      },
      stepList: codes.map((code, i) => ({
        code,
        label: label(code),
        done: order.status === "CANCELLED" ? false : i < idx,
        current: order.status !== "CANCELLED" && i === idx,
      })),
      advanceText: advanceLabel(order.status, order.fulfillment, {
        customerArrived: arrived,
      }),
      itemsText: itemsLine(order.items),
      fulfillText: fulfillLine(order),
      contactText: contactLine(order),
    });
  },
  async advance() {
    try {
      await api.merchantAdvance(this.id, this.data.order.status);
      await this.refresh();
    } catch (e) {}
  },
  async cancel() {
    const ok = await askCancel(this.id);
    if (!ok) return;
    try {
      await requestSubscribe(["cancelTemplateId"]);
      await api.merchantCancelOrder(this.id, "商家取消");
      await this.refresh();
    } catch (e) {}
  },
});

function askCancel(id) {
  return new Promise((resolve) => {
    wx.showModal({
      title: copy.merchant.cancelConfirmTitle,
      content: copy.format(copy.merchant.cancelConfirm, { id }),
      confirmText: copy.merchant.cancelConfirmOk,
      cancelText: copy.merchant.cancelConfirmBack,
      confirmColor: "#DC2626",
      success: (res) => resolve(!!res.confirm),
      fail: () => resolve(false),
    });
  });
}
