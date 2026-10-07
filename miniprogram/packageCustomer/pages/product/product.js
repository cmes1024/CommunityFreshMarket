const api = require("../../../utils/api");
const config = require("../../../utils/config");
const copy = require("../../../utils/copy");
const { fenToYuan } = require("../../../utils/money");
const { syncCartCount } = require("../../../utils/cartSync");
const { detailUnitLine } = require("../../../utils/productUnitLine");

Page({
  data: { product: {}, qty: 1, watching: false },
  async onLoad(query) {
    this.id = Number(query.id);
    const product = await api.product(this.id);
    this.setData({
      product: {
        ...product,
        priceYuan: fenToYuan(product.priceFen),
        unitLine: detailUnitLine(product),
      },
    });
    if (!product.inStock) {
      try {
        const st = await api.restockStatus(this.id);
        this.setData({ watching: !!st.watching });
      } catch (e) {}
    }
  },
  onStepper(e) {
    this.setData({ qty: e.detail.value });
  },
  async addCart() {
    await api.cartAdd(this.id, this.data.qty);
    await syncCartCount();
    wx.showToast({ title: copy.product.addedToast, icon: "none" });
  },
  async watch() {
    if (this.data.watching) {
      wx.showToast({ title: copy.product.subscribedToast, icon: "none" });
      return;
    }
    const tmplId = config.restockTemplateId;
    if (tmplId) {
      try {
        await new Promise((resolve) => {
          wx.requestSubscribeMessage({
            tmplIds: [tmplId],
            complete: resolve,
          });
        });
      } catch (e) {}
    }
    await api.restockWatch(this.id);
    this.setData({ watching: true });
    wx.showToast({ title: copy.product.restockToast, icon: "none" });
  },
});
