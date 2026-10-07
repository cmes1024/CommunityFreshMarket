const api = require("../../../utils/api");
const { fenToYuan } = require("../../../utils/money");

Page({
  data: { list: [] },
  async onShow() {
    const list = await api.orders();
    this.setData({
      list: (list || []).map((o) => ({ ...o, totalYuan: fenToYuan(o.totalAmountFen) })),
    });
  },
  goDetail(e) {
    wx.navigateTo({
      url: `/packageCustomer/pages/order-detail/order-detail?id=${e.currentTarget.dataset.id}`,
    });
  },
});
