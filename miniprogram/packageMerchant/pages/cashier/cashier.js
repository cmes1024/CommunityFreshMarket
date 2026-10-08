const api = require("../../../utils/api");
const copy = require("../../../utils/copy");
const { fenToYuan } = require("../../../utils/money");

Page({
  data: {
    sale: null,
    openList: [],
    products: [],
    checkins: [],
    addProductId: "",
    addQty: "1",
  },
  async onShow() {
    await Promise.all([this.ensureSale(), this.loadCheckins(), this.loadProducts()]);
  },
  async loadProducts() {
    try {
      const products = await api.merchantProducts();
      this.setData({ products: products || [] });
    } catch (e) {}
  },
  async loadCheckins() {
    try {
      const checkins = await api.merchantCheckinsActive();
      this.setData({ checkins: checkins || [] });
    } catch (e) {}
  },
  mapSale(sale) {
    if (!sale) return null;
    return {
      ...sale,
      totalYuan: fenToYuan(sale.totalFen),
      payYuan: fenToYuan(sale.payFen || sale.totalFen),
      items: (sale.items || []).map((it) => ({
        ...it,
        unitYuan: fenToYuan(it.unitPriceFen),
        amountYuan: fenToYuan(it.amountFen),
      })),
    };
  },
  async ensureSale() {
    try {
      const openList = await api.merchantCashierOpenList();
      let sale = openList && openList[0];
      if (!sale) {
        sale = await api.merchantCashierOpen();
      }
      this.setData({
        openList: openList || [],
        sale: this.mapSale(sale),
      });
    } catch (e) {}
  },
  async newSale() {
    try {
      const sale = await api.merchantCashierOpen();
      this.setData({ sale: this.mapSale(sale) });
    } catch (e) {}
  },
  onAddProductId(e) {
    this.setData({ addProductId: e.detail.value });
  },
  onAddQty(e) {
    this.setData({ addQty: e.detail.value });
  },
  pickProduct(e) {
    this.setData({ addProductId: String(e.currentTarget.dataset.id) });
  },
  async addItem() {
    const sale = this.data.sale;
    if (!sale) return;
    const productId = Number(this.data.addProductId);
    const qty = Number(this.data.addQty) || 1;
    if (!productId) {
      wx.showToast({ title: "请选择商品", icon: "none" });
      return;
    }
    try {
      const next = await api.merchantCashierAddItem(sale.id, { productId, qty });
      this.setData({ sale: this.mapSale(next), addProductId: "", addQty: "1" });
    } catch (e) {}
  },
  async attachCheckin(e) {
    const sale = this.data.sale;
    if (!sale) return;
    const checkinId = e.currentTarget.dataset.id;
    try {
      const next = await api.merchantCashierMember(sale.id, { checkinId });
      this.setData({ sale: this.mapSale(next) });
      wx.showToast({ title: "已挂会员", icon: "none" });
    } catch (err) {}
  },
  async completeSale() {
    const sale = this.data.sale;
    if (!sale) return;
    try {
      await api.merchantCashierComplete(sale.id, {});
      wx.showToast({ title: copy.merchantCashier.complete, icon: "none" });
      await this.ensureSale();
    } catch (e) {}
  },
  async cancelSale() {
    const sale = this.data.sale;
    if (!sale) return;
    try {
      await api.merchantCashierCancel(sale.id);
      await this.ensureSale();
    } catch (e) {}
  },
});
