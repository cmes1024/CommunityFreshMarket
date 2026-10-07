const api = require("../../../utils/api");

Page({
  data: { list: [], pick: false },
  onLoad(query) {
    this.setData({ pick: query.pick === "1" });
  },
  async onShow() {
    this.setData({ list: await api.addresses() });
  },
  add() {
    wx.navigateTo({ url: "/packageCustomer/pages/address-edit/address-edit" });
  },
  onTap(e) {
    const id = e.currentTarget.dataset.id;
    const item = this.data.list.find((a) => a.id === id);
    if (this.data.pick) {
      getApp().globalData.pickedAddress = item;
      wx.navigateBack();
      return;
    }
    wx.navigateTo({ url: `/packageCustomer/pages/address-edit/address-edit?id=${id}` });
  },
});
