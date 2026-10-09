const api = require("../../../utils/api");
const { fenToYuan, yuanPlain } = require("../../../utils/money");
const { syncCartCount, qtyMap } = require("../../../utils/cartSync");
const copy = require("../../../utils/copy");
const { listUnitLine } = require("../../../utils/productUnitLine");
const { openProduct } = require("../../../utils/productOpen");
const { qtyBounds, normalizeCartQty } = require("../../../utils/productQty");
const { storeDisplayName } = copy;

function heroImagesOf(store) {
  const list = store && store.heroImageUrls;
  if (Array.isArray(list) && list.length) {
    return list.map((url) => String(url || "").trim()).filter(Boolean).slice(0, 3);
  }
  const one = store && store.heroImageUrl;
  return one ? [one] : [];
}

function noticeDuration(text) {
  const n = String(text || "").trim().length;
  return Math.max(8, Math.round(n * 0.45));
}
function normalizeCols(cols) {
  const n = Number(cols);
  return n === 1 || n === 3 ? n : 2;
}

function deliverySubText(store) {
  const min = Number(store && store.minDeliveryFen) || 0;
  if (min > 0) return copy.format(copy.home.deliveryMin, { yuan: yuanPlain(min) });
  return copy.home.deliverySub;
}

Page({
  data: {
    store: {},
    displayStoreName: copy.storeName,
    homeKicker: copy.home.kicker,
    homeTitle: "",
    homeSubtitle: copy.home.heroDesc,
    homeNotice: "",
    noticeAnim: {},
    heroImages: [],
    homeProductCols: 2,
    categories: [],
    products: [],
    pickupHint: "",
    deliverySub: copy.home.deliverySub,
    sheetShow: false,
    sheetProductId: 0,
  },
  async onShow() {
    await this.load();
  },
  async load() {
    try {
      const [store, categories, products, cart, slots] = await Promise.all([
        api.store(),
        api.categories(),
        api.products({ homeRecommended: true }),
        syncCartCount(),
        api.pickupSlots().catch(() => []),
      ]);
      const qtys = qtyMap(cart);
      const name = storeDisplayName(store);
      const ranked = (products || []).map((p) => {
        const bounds = qtyBounds(p, { forDetail: false });
        const minAdd = bounds.min > 0 ? bounds.min : 1;
        return {
          ...p,
          priceYuan: fenToYuan(p.priceFen),
          unitLine: listUnitLine(p),
          qty: qtys[p.id] || 0,
          qtyMax: bounds.max,
          qtyMinAdd: minAdd,
        };
      });
      this.setData({
        store,
        displayStoreName: name,
        homeKicker: (store && store.homeKicker) || copy.home.kicker,
        // 主标题与导航店名分工：有配置才显示，空则不显示，避免重复
        homeTitle: (store && store.homeTitle) || "",
        homeSubtitle: (store && store.homeSubtitle) || copy.home.heroDesc,
        homeNotice: (store && store.homeNotice) || "",
        noticeAnim: {},
        heroImages: heroImagesOf(store),
        homeProductCols: normalizeCols(store && store.homeProductCols),
        categories,
        deliverySub: deliverySubText(store),
        pickupHint: (slots && slots[0] && slots[0].label) || this.data.copy.home.pickupSubFallback,
        products: ranked,
      }, () => this.startNotice());
    } catch (e) {
      this.clearNotice();
      this.setData({ displayStoreName: storeDisplayName(null) });
    }
  },
  onHide() {
    this.clearNotice();
  },
  onUnload() {
    this.clearNotice();
  },
  clearNotice() {
    if (this._noticeTimer) {
      clearTimeout(this._noticeTimer);
      this._noticeTimer = null;
    }
  },
  startNotice() {
    this.clearNotice();
    const text = this.data.homeNotice;
    if (!text) return;
    wx.nextTick(() => {
      wx.createSelectorQuery()
        .in(this)
        .select(".notice-item")
        .boundingClientRect((rect) => {
          if (!rect || !rect.width || this.data.homeNotice !== text) return;
          this.loopNotice(text, Math.ceil(rect.width));
        })
        .exec();
    });
  },
  loopNotice(text, distance) {
    if (this.data.homeNotice !== text) return;
    const duration = noticeDuration(text) * 1000;
    const anim = wx.createAnimation({ duration, timingFunction: "linear" });
    anim.translateX(-distance).step();
    this.setData({ noticeAnim: anim.export() });
    this._noticeTimer = setTimeout(() => {
      if (this.data.homeNotice !== text) return;
      const reset = wx.createAnimation({ duration: 0 });
      reset.translateX(0).step();
      this.setData({ noticeAnim: reset.export() });
      this._noticeTimer = setTimeout(() => this.loopNotice(text, distance), 30);
    }, duration);
  },
  goAll() {
    wx.reLaunch({ url: "/packageCustomer/pages/category/category" });
  },
  goCategory(e) {
    const id = e.currentTarget.dataset.id;
    wx.reLaunch({ url: `/packageCustomer/pages/category/category?categoryId=${id}` });
  },
  goDetail(e) {
    openProduct(this, e.currentTarget.dataset.id, this.data.store);
  },
  onSheetClose() {
    if (this.data.sheetShow) this.setData({ sheetShow: false });
  },
  async onSheetChanged() {
    try {
      const cart = await syncCartCount();
      const qtys = qtyMap(cart);
      const products = (this.data.products || []).map((p) => ({
        ...p,
        qty: qtys[p.id] || 0,
      }));
      this.setData({ products });
    } catch (e) {}
  },
  noop() {},
  async onQty(e) {
    const id = e.currentTarget.dataset.id;
    const minAdd = Number(e.currentTarget.dataset.min) || 1;
    const max = Number(e.currentTarget.dataset.max) || 999;
    let qty = normalizeCartQty(e.detail.value, minAdd);
    qty = Math.min(max, qty);
    try {
      const cart = await api.cartSetQty(id, qty);
      await syncCartCount(cart);
      const idx = this.data.products.findIndex((p) => p.id === id);
      if (idx >= 0) this.setData({ [`products[${idx}].qty`]: qty });
    } catch (err) {}
  },
});
