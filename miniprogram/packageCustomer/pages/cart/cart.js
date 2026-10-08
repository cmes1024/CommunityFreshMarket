const api = require("../../../utils/api");
const copy = require("../../../utils/copy");
const { fenToYuan } = require("../../../utils/money");
const { syncCartCount } = require("../../../utils/cartSync");

/** 价格后后缀：份 · 约 10 枚 / 斤 · 当面称重结 */
function priceHint(item) {
  const label = (item && item.unitLabel) || "";
  const note = item && item.specNote && String(item.specNote).trim();
  if (note) return label ? `${label} · ${note}` : note;
  if (item && item.unit === "JIN") {
    return label ? `${label} · ${copy.home.weighHint}` : copy.home.weighHint;
  }
  return label;
}

function decorate(cart) {
  return {
    ...cart,
    totalYuan: fenToYuan(cart.totalFen),
    items: (cart.items || []).map((i) => ({
      ...i,
      priceYuan: fenToYuan(i.priceFen),
      qtyNum: Number(i.qty),
      priceHint: priceHint(i),
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
    try {
      const cart = await api.cartSetQty(e.currentTarget.dataset.id, e.detail.value);
      await syncCartCount();
      this.setData({ cart: decorate(cart) });
    } catch (e) {}
  },
  async remove(e) {
    try {
      const cart = await api.cartRemove(e.currentTarget.dataset.id);
      await syncCartCount();
      this.setData({ cart: decorate(cart) });
    } catch (e) {}
  },
  checkout() {
    if (!this.data.cart.availableCount) {
      wx.showToast({ title: copy.cart.noBookableToast, icon: "none" });
      return;
    }
    wx.navigateTo({ url: "/packageCustomer/pages/checkout/checkout" });
  },
});
