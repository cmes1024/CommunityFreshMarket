const api = require("../../../utils/api");
const copy = require("../../../utils/copy");
const { fenToYuan } = require("../../../utils/money");
const { syncCartCount } = require("../../../utils/cartSync");

function decorate(cart) {
  return {
    ...cart,
    totalYuan: fenToYuan(cart.totalFen),
    items: (cart.items || []).map((i) => ({
      ...i,
      priceYuan: fenToYuan(i.priceFen),
      qtyNum: Number(i.qty),
    })),
  };
}

Page({
  data: { cart: { itemCount: 0, items: [] } },
  async onShow() {
    const cart = await syncCartCount();
    this.setData({ cart: decorate(cart || { itemCount: 0, items: [] }) });
  },
  goCategory() {
    wx.reLaunch({ url: "/packageCustomer/pages/category/category" });
  },
  async onQty(e) {
    const cart = await api.cartSetQty(e.currentTarget.dataset.id, e.detail.value);
    await syncCartCount();
    this.setData({ cart: decorate(cart) });
  },
  async remove(e) {
    const cart = await api.cartRemove(e.currentTarget.dataset.id);
    await syncCartCount();
    this.setData({ cart: decorate(cart) });
  },
  checkout() {
    if (!this.data.cart.availableCount) {
      wx.showToast({ title: copy.cart.noBookableToast, icon: "none" });
      return;
    }
    wx.navigateTo({ url: "/packageCustomer/pages/checkout/checkout" });
  },
});
