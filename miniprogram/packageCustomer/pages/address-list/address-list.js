const api = require("../../../utils/api");

Page({
  data: { list: [], pick: false, loading: true, loadError: false },
  onLoad(query) {
    // 首帧先出上次的地址簿，返回键和选地址都不用等白屏
    const cached = api.peekAddresses() || [];
    this.setData({
      pick: query.pick === "1",
      list: cached,
      loading: !cached.length,
    });
  },
  async onShow() {
    await this.reload();
  },
  async reload() {
    this.setData({ loadError: false });
    try {
      const list = await api.addresses();
      this.setData({ list: list || [], loading: false });
    } catch (e) {
      this.setData({ loading: false, loadError: true });
    }
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
