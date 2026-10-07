const api = require("../../utils/api");
const config = require("../../utils/config");
const copy = require("../../utils/copy");
const { fenToYuan } = require("../../utils/money");
const { syncCartCount } = require("../../utils/cartSync");
const { detailUnitLine } = require("../../utils/productUnitLine");

Component({
  properties: {
    show: { type: Boolean, value: false },
    productId: { type: Number, optionalTypes: [String], value: 0 },
  },
  data: {
    copy,
    loading: false,
    product: null,
    qty: 1,
    watching: false,
  },
  observers: {
    "show, productId"(visible, productId) {
      if (visible && productId) {
        this.load(productId);
        return;
      }
      if (!visible) {
        this.setData({ product: null, qty: 1, watching: false, loading: false });
      }
    },
  },
  methods: {
    async load(id) {
      const pid = Number(id);
      if (!pid) return;
      this.setData({ loading: true, product: null, qty: 1, watching: false });
      try {
        const product = await api.product(pid);
        const next = {
          loading: false,
          product: {
            ...product,
            priceYuan: fenToYuan(product.priceFen),
            unitLine: detailUnitLine(product),
          },
          qty: 1,
          watching: false,
        };
        if (!product.inStock) {
          try {
            const st = await api.restockStatus(pid);
            next.watching = !!st.watching;
          } catch (e) {}
        }
        this.setData(next);
      } catch (e) {
        this.setData({ loading: false, product: null });
        this.close();
      }
    },
    onStepper(e) {
      this.setData({ qty: e.detail.value });
    },
    async addCart() {
      const p = this.data.product;
      if (!p || !p.inStock) return;
      try {
        await api.cartAdd(p.id, this.data.qty);
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
    close() {
      this.triggerEvent("close");
    },
    onAfterLeave() {
      this.triggerEvent("close");
    },
    noop() {},
  },
});
