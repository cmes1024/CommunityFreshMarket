const api = require("../../utils/api");
const config = require("../../utils/config");
const copy = require("../../utils/copy");
const { fenToYuan } = require("../../utils/money");
const { syncCartCount } = require("../../utils/cartSync");
const { detailUnitLine } = require("../../utils/productUnitLine");
const { gallerySlides } = require("../../utils/livePhoto");
const { qtyBounds, initialDetailQty, normalizeCartQty } = require("../../utils/productQty");

Component({
  properties: {
    show: { type: Boolean, value: false },
    productId: { type: Number, optionalTypes: [String], value: 0 },
  },
  data: {
    copy,
    loading: false,
    product: null,
    slides: [],
    qty: 1,
    qtyMin: 1,
    qtyMax: 999,
    watching: false,
    contactNote: "",
    liveRequesting: false,
  },
  observers: {
    "show, productId"(visible, productId) {
      if (visible && productId) {
        this.load(productId);
        return;
      }
      if (!visible) {
        this.setData({
          product: null,
          slides: [],
          qty: 1,
          watching: false,
          loading: false,
          contactNote: "",
        });
      }
    },
  },
  methods: {
    async load(id) {
      const pid = Number(id);
      if (!pid) return;
      this.setData({
        loading: true,
        product: null,
        slides: [],
        qty: 1,
        watching: false,
        contactNote: "",
      });
      try {
        const [product, store] = await Promise.all([
          api.product(pid),
          api.store().catch(() => null),
        ]);
        const bounds = qtyBounds(product, { forDetail: true });
        const next = {
          loading: false,
          product: {
            ...product,
            priceYuan: fenToYuan(product.priceFen),
            unitLine: detailUnitLine(product),
          },
          slides: gallerySlides(product),
          qty: initialDetailQty(product),
          qtyMin: bounds.min,
          qtyMax: bounds.max,
          watching: false,
          contactNote: ((store && store.contactNote) || "").trim(),
          liveRequesting: false,
        };
        if (!product.inStock) {
          try {
            const st = await api.restockStatus(pid);
            next.watching = !!st.watching;
          } catch (e) {}
        }
        this.setData(next);
      } catch (e) {
        this.setData({ loading: false, product: null, contactNote: "" });
        this.close();
      }
    },
    onStepper(e) {
      this.setData({ qty: e.detail.value });
    },
    async addCart() {
      const p = this.data.product;
      if (!p || !p.inStock) return;
      const qty = normalizeCartQty(this.data.qty, this.data.qtyMin);
      try {
        await api.cartAdd(p.id, qty);
        await syncCartCount();
        this.triggerEvent("changed", { productId: p.id });
        wx.showToast({ title: copy.product.addedToast, icon: "none" });
      } catch (e) {}
    },
    async watch() {
      const p = this.data.product;
      if (!p) return;
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
            fail: resolve,
          });
          });
        } catch (e) {}
      }
      try {
        await api.restockWatch(p.id);
        this.setData({ watching: true });
        wx.showToast({ title: copy.product.restockToast, icon: "none" });
      } catch (e) {}
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
    close() {
      this.triggerEvent("close");
    },
    onAfterLeave() {
      this.triggerEvent("close");
    },
    noop() {},
  },
});
