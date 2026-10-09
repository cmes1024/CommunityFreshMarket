const api = require("../../../utils/api");
const copy = require("../../../utils/copy");
const { chooseAndUpload } = require("../../../utils/upload");
const { productDetailMode } = require("../../../utils/productOpen");
const { memberCodeEntry } = require("../../../utils/memberCodeEntry");
const { yuanPlain } = require("../../../utils/money");
const { formatRatio, parseRatio, sanitizeRatioInput } = require("../../../utils/pointsRatio");

function stickyTopPx() {
  try {
    const menu = wx.getMenuButtonBoundingClientRect();
    return (Number(menu.top) || 0) + (Number(menu.height) || 32) + 6;
  } catch (e) {
    return 88;
  }
}

function phraseList(raw) {
  const list = Array.isArray(raw) ? raw : [];
  return list.map((text) => String(text || "").trim()).filter(Boolean);
}

function phraseFields(kind) {
  return kind === "pickup"
    ? { list: "pickupRemarkPhrases", draft: "pickupPhraseDraft" }
    : { list: "deliveryRemarkPhrases", draft: "deliveryPhraseDraft" };
}

function heroImagesOf(store) {
  const list = store && store.heroImageUrls;
  if (Array.isArray(list) && list.length) {
    return list.map((url) => String(url || "").trim()).filter(Boolean).slice(0, 3);
  }
  const one = store && store.heroImageUrl;
  return one ? [one] : [];
}
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
    homeNotice: store.homeNotice || "",
    minDeliveryYuan: Number(store.minDeliveryFen) > 0 ? yuanPlain(store.minDeliveryFen) : "",
    pointsPerYuan: store.pointsPerYuan == null ? "" : formatRatio(store.pointsPerYuan),
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
    memberCodeEntry: "PROFILE",
    settingsTab: "store",
    stickyTop: 88,
    phraseKind: "delivery",
    heroImages: [],
    deliveryRemarkPhrases: [],
    pickupRemarkPhrases: [],
    deliveryPhraseDraft: "",
    pickupPhraseDraft: "",
  },
  async onShow() {
    this.setData({ copy, stickyTop: stickyTopPx() });
    await this.reload();
  },
  async reload() {
    const [store, slots] = await Promise.all([api.store(), api.merchantPickupSlots()]);
    this.setData({
      store: formStore(store),
      heroImages: heroImagesOf(store),
      slots,
      homeProductCols: normalizeCols(store.homeProductCols),
      productDetailMode: productDetailMode(store),
      memberCodeEntry: memberCodeEntry(store),
      deliveryRemarkPhrases: phraseList(
        store.deliveryRemarkPhrases != null ? store.deliveryRemarkPhrases : store.remarkPhrases
      ),
      pickupRemarkPhrases: phraseList(
        store.pickupRemarkPhrases != null ? store.pickupRemarkPhrases : store.remarkPhrases
      ),
      deliveryPhraseDraft: "",
      pickupPhraseDraft: "",
    });
  },
  onStore(e) {
    this.setData({ [`store.${e.currentTarget.dataset.k}`]: e.detail.value });
  },
  onPoints(e) {
    const value = sanitizeRatioInput(e.detail.value);
    this.setData({ "store.pointsPerYuan": value });
    return value;
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
  onMemberCodeEntry(e) {
    this.setData({ memberCodeEntry: memberCodeEntry({ memberCodeEntry: e.currentTarget.dataset.entry }) });
  },
  onSettingsTab(e) {
    const tab = e.currentTarget.dataset.tab;
    if (!tab || tab === this.data.settingsTab) return;
    this.setData({ settingsTab: tab });
  },
  onPhraseKind(e) {
    const kind = e.currentTarget.dataset.kind === "pickup" ? "pickup" : "delivery";
    if (kind === this.data.phraseKind) return;
    this.setData({ phraseKind: kind });
  },
  onPhraseDraft(e) {
    const fields = phraseFields(e.currentTarget.dataset.kind);
    this.setData({ [fields.draft]: e.detail.value });
  },
  addPhrase(e) {
    const fields = phraseFields(e.currentTarget.dataset.kind);
    const text = String(this.data[fields.draft] || "").trim();
    if (!text) return;
    if (text.length > 8) {
      wx.showToast({ title: copy.settings.phraseTooLong, icon: "none" });
      return;
    }
    const list = this.data[fields.list] || [];
    if (list.indexOf(text) >= 0) {
      this.setData({ [fields.draft]: "" });
      return;
    }
    if (list.length >= 8) {
      wx.showToast({ title: copy.settings.phraseTooMany, icon: "none" });
      return;
    }
    this.setData({ [fields.list]: list.concat(text), [fields.draft]: "" });
  },
  removePhrase(e) {
    const fields = phraseFields(e.currentTarget.dataset.kind);
    const text = e.currentTarget.dataset.text;
    this.setData({
      [fields.list]: (this.data[fields.list] || []).filter((item) => item !== text),
    });
  },
  onSlotLabel(e) {
    this.setData({ slotLabel: e.detail.value });
  },
  showAddSlot() {
    this.setData({ addingSlot: true });
  },
  async pickHero() {
    const left = 3 - (this.data.heroImages || []).length;
    if (left <= 0) {
      wx.showToast({ title: copy.settings.heroTooMany, icon: "none" });
      return;
    }
    try {
      const uploaded = await chooseAndUpload(left);
      const added = Array.isArray(uploaded) ? uploaded : [uploaded];
      await this.saveHeroes((this.data.heroImages || []).concat(added).slice(0, 3));
      wx.showToast({ title: copy.settings.heroSaved, icon: "none" });
    } catch (e) {}
  },
  async removeHero(e) {
    const index = Number(e.currentTarget.dataset.index);
    const next = (this.data.heroImages || []).slice();
    if (index < 0 || index >= next.length) return;
    next.splice(index, 1);
    try {
      await this.saveHeroes(next);
      wx.showToast({ title: next.length ? copy.settings.heroSaved : copy.settings.heroCleared, icon: "none" });
    } catch (err) {}
  },
  async saveHeroes(urls) {
    const heroImageUrls = (urls || []).slice(0, 3);
    const store = await api.merchantUpdateStore({ heroImageUrls });
    this.setData({
      store: formStore(store),
      heroImages: heroImagesOf(store),
      homeProductCols: normalizeCols(store.homeProductCols),
      productDetailMode: productDetailMode(store),
      memberCodeEntry: memberCodeEntry(store),
    });
  },
  async saveStore() {
    const { name, contactPhone, contactNote, homeKicker, homeTitle, homeSubtitle, homeNotice, minDeliveryYuan } = this.data.store;
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
    const codeEntry = memberCodeEntry({ memberCodeEntry: this.data.memberCodeEntry });
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
      homeNotice: homeNotice == null ? "" : String(homeNotice).trim(),
      homeProductCols: cols,
      productDetailMode: detailMode,
      memberCodeEntry: codeEntry,
      deliveryRemarkPhrases: this.data.deliveryRemarkPhrases || [],
      pickupRemarkPhrases: this.data.pickupRemarkPhrases || [],
    };
    if (pointsPerYuan !== undefined && pointsPerYuan !== "") {
      const ratio = parseRatio(pointsPerYuan);
      if (ratio == null) {
        wx.showToast({ title: copy.settings.pointsInvalid, icon: "none" });
        return;
      }
      payload.pointsPerYuan = ratio;
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
        memberCodeEntry: Object.prototype.hasOwnProperty.call(store, "memberCodeEntry")
          ? memberCodeEntry(store)
          : codeEntry,
        deliveryRemarkPhrases: Object.prototype.hasOwnProperty.call(store, "deliveryRemarkPhrases")
          ? phraseList(store.deliveryRemarkPhrases)
          : this.data.deliveryRemarkPhrases,
        pickupRemarkPhrases: Object.prototype.hasOwnProperty.call(store, "pickupRemarkPhrases")
          ? phraseList(store.pickupRemarkPhrases)
          : this.data.pickupRemarkPhrases,
        deliveryPhraseDraft: "",
        pickupPhraseDraft: "",
      });
      getApp().globalData.memberCodeEntry = Object.prototype.hasOwnProperty.call(store, "memberCodeEntry")
        ? memberCodeEntry(store)
        : codeEntry;
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
