const api = require("../../utils/api");
const copy = require("../../utils/copy");
const { fenToYuan } = require("../../utils/money");
const { syncCartCount, lastCart, setQty } = require("../../utils/cartSync");
const { goTab } = require("../../utils/nav");

/** 价格后后缀：份 · 约 10 枚 / 斤 · 以实际重量为准 */
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

/** 行消失时的本地兜底：总价、行数、可下单行数一起改，避免和服务端结果打架 */
function removedPatch(cart, item) {
  const items = (cart.items || []).filter((i) => i.productId !== item.productId);
  const minus = item.available ? (item.priceFen || 0) * (Number(item.qtyNum) || 0) : 0;
  const totalFen = Math.max(0, (cart.totalFen || 0) - minus);
  return {
    "cart.items": items,
    "cart.itemCount": items.length,
    "cart.availableCount": Math.max(0, (cart.availableCount || 0) - (item.available ? 1 : 0)),
    "cart.totalFen": totalFen,
    "cart.totalYuan": fenToYuan(totalFen),
  };
}

Page({
  data: { cart: { itemCount: 0, items: [] } },
  onLoad() {
    // 首帧先用上次的购物篮填满，再拉服务端校准
    const snap = lastCart();
    if (snap) this.setData({ cart: decorate(snap) });
  },
  onShow() {
    this.refresh();
  },
  async refresh() {
    const cart = await syncCartCount();
    if (cart) this.setData({ cart: decorate(cart) });
  },
  goCategory() {
    goTab("/pages/category/category");
  },
  onQty(e) {
    const id = Number(e.currentTarget.dataset.id);
    const value = Number(e.detail.value) || 0;
    const cart = this.data.cart;
    const items = cart.items || [];
    const idx = items.findIndex((i) => i.productId === id);
    if (idx < 0) return;
    const item = items[idx];
    const prev = Number(item.qtyNum) || 0;
    if (prev === value) return;

    if (value === 0) {
      this.setData(removedPatch(cart, item));
    } else {
      const delta = (value - prev) * (item.priceFen || 0);
      const totalFen = Math.max(0, (cart.totalFen || 0) + delta);
      this.setData({
        [`cart.items[${idx}].qtyNum`]: value,
        [`cart.items[${idx}].qty`]: value,
        "cart.totalFen": totalFen,
        "cart.totalYuan": fenToYuan(totalFen),
      });
    }
    this.commitQty(id, value);
  },
  async commitQty(id, value) {
    try {
      const cart = await setQty(id, value);
      if (cart) this.setData({ cart: decorate(cart) });
    } catch (err) {
      this.refresh();
    }
  },
  remove(e) {
    const id = Number(e.currentTarget.dataset.id);
    const cart = this.data.cart;
    const item = (cart.items || []).find((i) => i.productId === id);
    if (!item) return;
    this.setData(removedPatch(cart, item));
    api
      .cartRemove(id)
      .then((next) => this.setData({ cart: decorate(next) }))
      .catch(() => this.refresh());
  },
  checkout() {
    if (!this.data.cart.availableCount) {
      wx.showToast({ title: copy.cart.noBookableToast, icon: "none" });
      return;
    }
    wx.navigateTo({ url: "/packageCustomer/pages/checkout/checkout" });
  },
});
