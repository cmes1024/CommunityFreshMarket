const api = require("../../utils/api");
const { fenToYuan } = require("../../utils/money");
const { syncCartCount, qtyMap, lastCart, setQty } = require("../../utils/cartSync");
const { listUnitLine } = require("../../utils/productUnitLine");
const { openProduct } = require("../../utils/productOpen");
const { qtyBounds, normalizeCartQty } = require("../../utils/productQty");

function decorate(products, qtys) {
  return (products || []).map((p) => {
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
}

function barOf(cart) {
  return {
    ...(cart || {}),
    itemCount: (cart && cart.itemCount) || 0,
    totalYuan: fenToYuan(cart && cart.totalFen),
  };
}

Page({
  data: {
    categories: [],
    products: [],
    activeId: 0,
    cart: {},
    store: {},
    sheetShow: false,
    sheetProductId: 0,
    sheetPreview: null,
  },
  onLoad(query) {
    this._preset = query.categoryId ? Number(query.categoryId) : 0;
    this._lists = {};
    this._qtys = qtyMap(lastCart());
    // 首帧直接用上次的分类和列表，切页不白屏
    const categories = api.peekCategories();
    if (!categories || !categories.length) return;
    const has = (id) => !!id && categories.some((c) => c.id === id);
    const activeId =
      (has(this._preset) && this._preset) || (categories[0] && categories[0].id) || 0;
    const products = api.peekProducts(activeId);
    if (products) this._lists[activeId] = products;
    this._renderedRef = products || null;
    this.setData({
      categories,
      store: api.peekStore() || {},
      activeId,
      products: decorate(products || [], this._qtys),
      cart: barOf(lastCart()),
    });
  },
  async onShow() {
    // 从首页点某个分类进来时，switchTab 不能带参数，从 globalData 取
    const app = getApp();
    const pending = app.globalData && app.globalData.pendingCategoryId;
    if (pending) {
      app.globalData.pendingCategoryId = 0;
      this._preset = Number(pending) || 0;
    }
    const [categories, store] = await Promise.all([
      api.categories(),
      api.store().catch(() => ({})),
    ]);
    const list = categories || [];
    const has = (id) => !!id && list.some((c) => c.id === id);
    const preset = this._preset;
    this._preset = 0;
    const activeId =
      (has(preset) && preset) ||
      (has(this.data.activeId) && this.data.activeId) ||
      (list[0] && list[0].id) ||
      0;
    this.setData({ categories: list, store, activeId });
    await this.loadProducts(activeId);
  },
  async loadProducts(categoryId) {
    const products = await api.products(categoryId);
    if (!this._lists) this._lists = {};
    this._lists[categoryId] = products || [];
    const snap = lastCart();
    this._qtys = qtyMap(snap);
    // 同一个分类、同一份缓存数据就不用再推一遍列表
    if (this._renderedRef !== products) {
      this._renderedRef = products;
      this.setData({
        products: decorate(products, this._qtys),
        cart: barOf(snap),
      });
    }
    // 购物篮数量后台校准，不挡列表上屏
    syncCartCount().then((cart) => {
      if (this.data.activeId !== categoryId) return;
      this.applyCart(cart);
    });
  },
  applyCart(cart) {
    if (!cart) return;
    const qtys = qtyMap(cart);
    this._qtys = qtys;
    const patch = { cart: barOf(cart) };
    (this.data.products || []).forEach((p, i) => {
      const next = qtys[p.id] || 0;
      if (p.qty !== next) patch[`products[${i}].qty`] = next;
    });
    this.setData(patch);
  },
  selectCat(e) {
    const id = Number(e.currentTarget.dataset.id);
    if (!id || id === this.data.activeId) return;
    this._preset = 0;
    const cached = this._lists && this._lists[id];
    if (cached) {
      // 已看过的分类直接出列表，网络在后台刷新
      this._renderedRef = cached;
      this.setData({ activeId: id, products: decorate(cached, this._qtys || {}) });
    } else {
      this.setData({ activeId: id });
    }
    this.loadProducts(id);
  },
  goDetail(e) {
    const pid = Number(e.currentTarget.dataset.id);
    const preview = (this.data.products || []).find((p) => p.id === pid) || null;
    openProduct(this, pid, this.data.store, preview);
  },
  onSheetClose() {
    if (this.data.sheetShow) this.setData({ sheetShow: false });
  },
  onSheetChanged(e) {
    const cart = e && e.detail && e.detail.cart;
    if (cart) {
      this.applyCart(cart);
      return;
    }
    syncCartCount().then((next) => this.applyCart(next));
  },
  goCheckout() {
    wx.navigateTo({ url: "/packageCustomer/pages/checkout/checkout" });
  },
  async onQty(e) {
    const id = e.currentTarget.dataset.id;
    const minAdd = Number(e.currentTarget.dataset.min) || 1;
    const max = Number(e.currentTarget.dataset.max) || 999;
    let qty = normalizeCartQty(e.detail.value, minAdd);
    qty = Math.min(max, qty);
    const idx = (this.data.products || []).findIndex((p) => p.id === id);
    if (idx >= 0) {
      if (this.data.products[idx].qty === qty) return;
      this.setData({ [`products[${idx}].qty`]: qty });
    }
    try {
      const cart = await setQty(id, qty);
      this.applyCart(cart);
    } catch (err) {
      syncCartCount().then((cart) => this.applyCart(cart));
    }
  },
});
