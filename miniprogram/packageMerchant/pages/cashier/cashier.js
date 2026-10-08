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
    memberCoupons: [],
    discountYuan: "",
    adjustReason: "",
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
      couponYuan: fenToYuan(sale.couponDiscountFen || 0),
      payYuan: fenToYuan(sale.payFen != null ? sale.payFen : sale.totalFen),
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
      await this.loadMemberCoupons(sale && sale.memberUserId, sale && sale.id);
    } catch (e) {}
  },
  async loadMemberCoupons(userId, saleId) {
    if (!userId) {
      this.setData({ memberCoupons: [] });
      return;
    }
    try {
      const list = await api.merchantCouponUnused(userId, saleId);
      this.setData({
        memberCoupons: (list || []).map((c) => ({
          ...c,
          faceYuan: fenToYuan(c.faceFen),
        })),
      });
    } catch (e) {
      this.setData({ memberCoupons: [] });
    }
  },
  async showSale(sale) {
    this.setData({ sale: this.mapSale(sale) });
    await this.loadMemberCoupons(sale && sale.memberUserId, sale && sale.id);
  },
  async newSale() {
    try {
      const sale = await api.merchantCashierOpen();
      this.setData({ discountYuan: "", adjustReason: "" });
      await this.showSale(sale);
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
      this.setData({ addProductId: "", addQty: "1" });
      await this.showSale(next);
    } catch (e) {}
  },
  async scanAdd() {
    const sale = this.data.sale;
    if (!sale) return;
    let code = "";
    try {
      const res = await new Promise((resolve, reject) => {
        wx.scanCode({ success: resolve, fail: reject });
      });
      code = String(res.result || "").trim();
    } catch (e) {
      return;
    }
    const hit = (this.data.products || []).find(
      (p) => String(p.id) === code || p.name === code
    );
    if (!hit) {
      wx.showToast({ title: copy.merchantCashier.notFound, icon: "none" });
      return;
    }
    try {
      const next = await api.merchantCashierAddItem(sale.id, { productId: hit.id, qty: 1 });
      await this.showSale(next);
    } catch (e) {}
  },
  async removeItem(e) {
    const sale = this.data.sale;
    if (!sale) return;
    try {
      const next = await api.merchantCashierRemoveItem(sale.id, e.currentTarget.dataset.id);
      await this.showSale(next);
    } catch (err) {}
  },
  async pickMemberCoupon(e) {
    const sale = this.data.sale;
    if (!sale) return;
    const claimId = Number(e.currentTarget.dataset.id) || null;
    try {
      const next = await api.merchantCashierCoupon(sale.id, { claimId });
      await this.showSale(next);
    } catch (err) {}
  },
  onDiscount(e) {
    this.setData({ discountYuan: e.detail.value });
  },
  onReason(e) {
    this.setData({ adjustReason: e.detail.value });
  },
  async attachCheckin(e) {
    const sale = this.data.sale;
    if (!sale) return;
    const checkinId = e.currentTarget.dataset.id;
    try {
      const next = await api.merchantCashierMember(sale.id, { checkinId });
      await this.showSale(next);
      wx.showToast({ title: "已挂会员", icon: "none" });
    } catch (err) {}
  },
  async completeSale() {
    const sale = this.data.sale;
    if (!sale) return;
    try {
      const yuan = Number(this.data.discountYuan);
      const discountFen = Number.isFinite(yuan) && yuan > 0 ? Math.round(yuan * 100) : 0;
      await api.merchantCashierComplete(sale.id, {
        discountFen,
        adjustReason: (this.data.adjustReason || "").trim(),
      });
      wx.showToast({ title: copy.merchantCashier.complete, icon: "none" });
      this.setData({ discountYuan: "", adjustReason: "" });
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
