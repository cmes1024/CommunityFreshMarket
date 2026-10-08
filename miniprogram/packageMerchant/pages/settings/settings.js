const api = require("../../../utils/api");
const copy = require("../../../utils/copy");
const { chooseAndUpload } = require("../../../utils/upload");
const { productDetailMode } = require("../../../utils/productOpen");
const { yuanPlain } = require("../../../utils/money");

function normalizeCols(cols) {
  const n = Number(cols);
  return n === 1 || n === 3 ? n : 2;
}

function formStore(store) {
  return {
    ...store,
    homeKicker: store.homeKicker || "",
    homeTitle: store.homeTitle || "",
    homeSubtitle: store.homeSubtitle || "",
    minDeliveryYuan: Number(store.minDeliveryFen) > 0 ? yuanPlain(store.minDeliveryFen) : "",
  };
}

function mergeHomeFields(store, payload) {
  const next = { ...store };
  ["homeKicker", "homeTitle", "homeSubtitle"].forEach((key) => {
    next[key] = Object.prototype.hasOwnProperty.call(store, key)
      ? store[key] || ""
      : payload[key] || "";
  });
  return next;
}

Page({
  data: {
    store: {},
    slots: [],
    slotLabel: "",
    addingSlot: false,
    homeProductCols: 2,
    productDetailMode: "SHEET",
  },
  async onShow() {
    this.setData({ copy });
    await this.reload();
  },
  async reload() {
    const [store, slots] = await Promise.all([api.store(), api.merchantPickupSlots()]);
    this.setData({
      store: formStore(store),
      slots,
      homeProductCols: normalizeCols(store.homeProductCols),
      productDetailMode: productDetailMode(store),
    });
  },
  onStore(e) {
    this.setData({ [`store.${e.currentTarget.dataset.k}`]: e.detail.value });
  },
  onKicker(e) {
    this.setData({ "store.homeKicker": e.currentTarget.dataset.text || "" });
  },
  onCols(e) {
    this.setData({ homeProductCols: normalizeCols(e.currentTarget.dataset.cols) });
  },
  onDetailMode(e) {
    this.setData({ productDetailMode: productDetailMode({ productDetailMode: e.currentTarget.dataset.mode }) });
  },
  onSlotLabel(e) {
    this.setData({ slotLabel: e.detail.value });
  },
  showAddSlot() {
    this.setData({ addingSlot: true });
  },
  async pickHero() {
    try {
      const url = await chooseAndUpload();
      const store = await api.merchantUpdateStore({ heroImageUrl: url });
      this.setData({
        store: formStore(store),
        homeProductCols: normalizeCols(store.homeProductCols),
        productDetailMode: productDetailMode(store),
      });
      wx.showToast({ title: copy.settings.heroSaved, icon: "none" });
    } catch (e) {}
  },
  async clearHero() {
    try {
      const store = await api.merchantUpdateStore({ heroImageUrl: "" });
      this.setData({
        store: formStore(store),
        homeProductCols: normalizeCols(store.homeProductCols),
        productDetailMode: productDetailMode(store),
      });
      wx.showToast({ title: copy.settings.heroCleared, icon: "none" });
    } catch (e) {}
  },
  async saveStore() {
    const { name, contactPhone, contactNote, homeKicker, homeTitle, homeSubtitle, minDeliveryYuan } = this.data.store;
    const minRaw = String(minDeliveryYuan ?? "").trim();
    let minDeliveryFen = 0;
    if (minRaw !== "") {
      const yuan = Number(minRaw);
      if (!Number.isFinite(yuan) || yuan < 0 || yuan > 100000) {
        wx.showToast({ title: copy.settings.minDeliveryInvalid, icon: "none" });
        return;
      }
      minDeliveryFen = Math.round(yuan * 100);
    }
    const cols = this.data.homeProductCols;
    const detailMode = this.data.productDetailMode;
    const { pointsPerYuan, pickImageTtlMinutes } = this.data.store;
    const ttl = Number(pickImageTtlMinutes);
    if (pickImageTtlMinutes === undefined || pickImageTtlMinutes === "" || !Number.isFinite(ttl) || ttl < 1) {
      wx.showToast({ title: copy.settings.pickTtlInvalid, icon: "none" });
      return;
    }
    const payload = {
      name,
      contactPhone,
      contactNote,
      minDeliveryFen,
      homeKicker: homeKicker == null ? "" : String(homeKicker),
      homeTitle: homeTitle == null ? "" : String(homeTitle),
      homeSubtitle: homeSubtitle == null ? "" : String(homeSubtitle),
      homeProductCols: cols,
      productDetailMode: detailMode,
    };
    if (pointsPerYuan !== undefined && pointsPerYuan !== "") {
      payload.pointsPerYuan = Number(pointsPerYuan);
    }
    payload.pickImageTtlMinutes = ttl;
    try {
      const store = await api.merchantUpdateStore(payload);
      this.setData({
        store: formStore(mergeHomeFields({ ...this.data.store, ...store }, payload)),
        homeProductCols: Object.prototype.hasOwnProperty.call(store, "homeProductCols")
          ? normalizeCols(store.homeProductCols)
          : cols,
        productDetailMode: Object.prototype.hasOwnProperty.call(store, "productDetailMode")
          ? productDetailMode(store)
          : detailMode,
      });
      wx.showToast({ title: copy.settings.saved, icon: "none" });
    } catch (e) {}
  },
  async addSlot() {
    const label = (this.data.slotLabel || "").trim();
    if (!label) return;
    try {
      await api.merchantPickupSlotCreate({ label, enabled: true });
      this.setData({ slotLabel: "", addingSlot: false });
      await this.reload();
    } catch (e) {}
  },
  async removeSlot(e) {
    try {
      await api.merchantPickupSlotDelete(e.currentTarget.dataset.id);
      await this.reload();
    } catch (e) {}
  },
});
