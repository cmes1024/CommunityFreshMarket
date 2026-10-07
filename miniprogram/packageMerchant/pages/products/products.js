const api = require("../../../utils/api");
const { fenToYuan } = require("../../../utils/money");

Page({
  data: { list: [] },
  async onShow() {
    await this.load();
  },
  async load() {
    const list = await api.merchantProducts();
    this.setData({
      list: (list || []).map((p) => ({ ...p, priceYuan: fenToYuan(p.priceFen) })),
    });
  },
  goCats() {
    wx.navigateTo({ url: "/packageMerchant/pages/categories/categories" });
  },
  edit(e) {
    wx.navigateTo({
      url: `/packageMerchant/pages/product-edit/product-edit?id=${e.currentTarget.dataset.id}`,
    });
  },
  create() {
    wx.navigateTo({ url: "/packageMerchant/pages/product-edit/product-edit" });
  },
  async toggleShelf(e) {
    const { id, on } = e.currentTarget.dataset;
    try {
      await api.merchantOnShelf(id, !(on === 1 || on === "1"));
      await this.load();
    } catch (err) {}
  },
  async toggleStock(e) {
    const { id, on } = e.currentTarget.dataset;
    try {
      await api.merchantInStock(id, !(on === 1 || on === "1"));
      await this.load();
    } catch (err) {}
  },
});

