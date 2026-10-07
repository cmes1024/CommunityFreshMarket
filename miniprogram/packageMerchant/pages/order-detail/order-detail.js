const api = require("../../../utils/api");
const { fenToYuan } = require("../../../utils/money");
const {
  label,
  steps,
  advanceLabel,
  itemsLine,
  fulfillLine,
  contactLine,
} = require("../../../utils/orderStatus");

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
      order: { ...order, totalYuan: fenToYuan(order.totalAmountFen) },
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
    try {
      await api.merchantCancelOrder(this.id, "商家取消");
      await this.refresh();
    } catch (e) {}
  },
});
