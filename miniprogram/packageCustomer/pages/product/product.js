const api = require("../../../utils/api");
const config = require("../../../utils/config");
const copy = require("../../../utils/copy");
const { fenToYuan } = require("../../../utils/money");
const { syncCartCount } = require("../../../utils/cartSync");
const { detailUnitLine } = require("../../../utils/productUnitLine");
const { gallerySlides } = require("../../../utils/livePhoto");
const { qtyBounds, initialDetailQty, normalizeCartQty } = require("../../../utils/productQty");

Page({
  data: {
    product: {},
    slides: [],
    qty: 1,
    qtyMin: 1,
    qtyMax: 999,
    watching: false,
    contactNote: "",
    liveRequesting: false,
  },
  async onLoad(query) {
    this.id = Number(query.id);
    const [product, store] = await Promise.all([
      api.product(this.id),
      api.store().catch(() => null),
    ]);
    const bounds = qtyBounds(product, { forDetail: true });
    this.setData({
      product: {
        ...product,
        priceYuan: fenToYuan(product.priceFen),
        unitLine: detailUnitLine(product),
      },
      slides: gallerySlides(product),
      contactNote: ((store && store.contactNote) || "").trim(),
      qty: initialDetailQty(product),
      qtyMin: bounds.min,
      qtyMax: bounds.max,
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
    const qty = normalizeCartQty(this.data.qty, this.data.qtyMin);
    try {
      await api.cartAdd(this.id, qty);
      await syncCartCount();
      wx.showToast({ title: copy.product.addedToast, icon: "none" });
    } catch (e) {}
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
  async requestLivePhoto() {
    const product = this.data.product;
    if (!product || this.data.liveRequesting) return;
    if (product.livePhotoRequestedByMe) {
      const n = product.livePhotoApplicantCount || 0;
      wx.showToast({
        title: copy.livePhoto.requested.replace("{n}", String(n)),
        icon: "none",
      });
      return;
    }
    this.setData({ liveRequesting: true });
    try {
      const res = await api.livePhotoRequest(product.id);
      const next = {
        ...product,
        livePhotoApplicantCount: res.applicantCount,
        livePhotoRequestedByMe: true,
      };
      this.setData({ product: next, slides: gallerySlides(next) });
      wx.showToast({ title: copy.livePhoto.notified, icon: "none" });
    } catch (e) {
    } finally {
      this.setData({ liveRequesting: false });
    }
  },
});
