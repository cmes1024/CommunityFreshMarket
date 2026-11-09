const api = require("../../../utils/api");
const config = require("../../../utils/config");
const copy = require("../../../utils/copy");
const { fenToYuan } = require("../../../utils/money");
const { syncCartCount } = require("../../../utils/cartSync");
const { detailUnitLine } = require("../../../utils/productUnitLine");
const { gallerySlides } = require("../../../utils/livePhoto");
const { qtyBounds, initialDetailQty, normalizeCartQty } = require("../../../utils/productQty");
const storeMode = require("../../../utils/storeMode");

Page({
  data: {
    product: {},
    slides: [],
    qty: 1,
    qtyMin: 1,
    qtyMax: 999,
    watching: false,
    contactNote: "",
    contactPhone: "",
    showcase: false,
    liveRequesting: false,
    /* 头图轮播开关：页面被盖住时停掉，避免后台继续按 3s 轮播 */
    slideAutoplay: true,
  },
  onLoad(query) {
    this.id = Number(query.id);
    const store = api.peekStore();
    this.setData({
      showcase: storeMode.isShowcase(store),
      contactPhone: (store && store.contactPhone) || "",
    });
    // 有缓存就先出内容，接口回来再补齐
    const cached = api.peekProduct(this.id);
    if (cached) this.paint(cached);
    this.load();
  },
  onShow() {
    if (!this.data.slideAutoplay) this.setData({ slideAutoplay: true });
  },
  onHide() {
    // 进客服、选地址等二级页时把轮播停掉，少一路后台绘制
    if (this.data.slideAutoplay) this.setData({ slideAutoplay: false });
  },
  async load() {
    const [product, store] = await Promise.all([
      api.product(this.id),
      api.store().catch(() => null),
    ]);
    this.paint(product, store);
    if (!product.inStock) {
      try {
        const st = await api.restockStatus(this.id);
        this.setData({ watching: !!st.watching });
      } catch (e) {}
    }
  },
  paint(product, store) {
    const bounds = qtyBounds(product, { forDetail: true });
    const patch = {
      product: {
        ...product,
        priceYuan: fenToYuan(product.priceFen),
        unitLine: detailUnitLine(product),
      },
      slides: gallerySlides(product),
      qtyMin: bounds.min,
      qtyMax: bounds.max,
    };
    if (!this._qtyTouched) patch.qty = initialDetailQty(product);
    if (store !== undefined) {
      patch.contactNote = ((store && store.contactNote) || "").trim();
      patch.contactPhone = ((store && store.contactPhone) || "").trim();
      if (store) patch.showcase = storeMode.apply(store);
    }
    this.setData(patch);
  },
  callStore() {
    const phone = (this.data.contactPhone || "").trim();
    if (!phone) {
      wx.showToast({ title: copy.showcase.noPhone, icon: "none" });
      return;
    }
    wx.makePhoneCall({ phoneNumber: phone });
  },
  onStepper(e) {
    this._qtyTouched = true;
    this.setData({ qty: e.detail.value });
  },
  async addCart() {
    const qty = normalizeCartQty(this.data.qty, this.data.qtyMin);
    try {
      const cart = await api.cartAdd(this.id, qty);
      await syncCartCount(cart);
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
