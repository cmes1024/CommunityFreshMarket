const api = require("../../../utils/api");

Page({
  data: { list: [], pick: false },
  onLoad(query) {
    // 首帧先出上次的地址簿，返回键和选地址都不用等白屏
    this.setData({ pick: query.pick === "1", list: api.peekAddresses() || [] });
  },
  async onShow() {
    try {
      const list = await api.addresses();
      if (list) this.setData({ list });
    } catch (e) {}
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
