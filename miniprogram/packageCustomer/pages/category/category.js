const api = require("../../../utils/api");
const { fenToYuan } = require("../../../utils/money");
const { syncCartCount, qtyMap } = require("../../../utils/cartSync");
const { listUnitLine } = require("../../../utils/productUnitLine");
const { openProduct } = require("../../../utils/productOpen");

Page({
  data: {
    categories: [],
    products: [],
    activeId: 0,
    cart: {},
    store: {},
    sheetShow: false,
    sheetProductId: 0,
  },
  async onLoad(query) {
    this._preset = query.categoryId ? Number(query.categoryId) : 0;
  },
  async onShow() {
    const [categories, store] = await Promise.all([api.categories(), api.store().catch(() => ({}))]);
    const activeId = this._preset || (categories[0] && categories[0].id);
    this.setData({ categories, store, activeId });
    await this.loadProducts(activeId);
  },
  async loadProducts(categoryId) {
    const [products, cart] = await Promise.all([api.products(categoryId), syncCartCount()]);
    const qtys = qtyMap(cart);
    this.setData({
      products: (products || []).map((p) => ({
        ...p,
        priceYuan: fenToYuan(p.priceFen),
        unitLine: listUnitLine(p),
        qty: qtys[p.id] || 0,
      })),
      cart: { ...cart, totalYuan: fenToYuan(cart?.totalFen) },
    });
  },
  selectCat(e) {
    const id = e.currentTarget.dataset.id;
    this._preset = id;
    this.setData({ activeId: id });
    this.loadProducts(id);
  },
  goDetail(e) {
    openProduct(this, e.currentTarget.dataset.id, this.data.store);
  },
  onSheetClose() {
    if (this.data.sheetShow) this.setData({ sheetShow: false });
  },
  async onSheetChanged() {
    await this.loadProducts(this.data.activeId);
  },
  goCheckout() {
    wx.navigateTo({ url: "/packageCustomer/pages/checkout/checkout" });
  },
  async onQty(e) {
    const id = e.currentTarget.dataset.id;
    const qty = e.detail.value;
    const cart = await api.cartSetQty(id, qty);
    await syncCartCount(cart);
    const idx = this.data.products.findIndex((p) => p.id === id);
    this.setData({
      [`products[${idx}].qty`]: qty,
      cart: { ...cart, totalYuan: fenToYuan(cart.totalFen) },
    });
  },
});
