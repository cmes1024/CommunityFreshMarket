const api = require("../../../utils/api");
const copy = require("../../../utils/copy");
const { fenToYuan } = require("../../../utils/money");

function todayStr() {
  const d = new Date();
  const p = (n) => (n < 10 ? "0" + n : "" + n);
  return d.getFullYear() + "-" + p(d.getMonth() + 1) + "-" + p(d.getDate());
}

function expireText(endsAt) {
  if (!endsAt) return "";
  return String(endsAt).replace("T", " ").slice(0, 10);
}

function isExpired(endsAt) {
  if (!endsAt) return false;
  const t = Date.parse(String(endsAt).replace(" ", "T"));
  return Number.isFinite(t) && t <= Date.now();
}

function yuanPlain(fen) {
  const n = (Number(fen) || 0) / 100;
  return String(Math.round(n * 100) / 100);
}

function offerText(c) {
  const face = yuanPlain(c.faceFen);
  if ((c.minSpendFen || 0) > 0) {
    return copy.format(copy.merchantCouponForm.offer, { min: yuanPlain(c.minSpendFen), face });
  }
  return copy.format(copy.merchantCouponForm.offerNoMin, { face });
}

Page({
  data: {
    title: "",
    faceYuan: "",
    minSpendYuan: "0",
    expireDate: "",
    today: todayStr(),
    list: [],
    sheetOpen: false,
  },
  async onShow() {
    await this.reload();
  },
  async reload() {
    try {
      const list = await api.merchantCoupons();
      this.setData({
        list: (list || []).map((c) => ({
          ...c,
          faceYuan: fenToYuan(c.faceFen),
          minYuan: fenToYuan(c.minSpendFen),
          offerText: offerText(c),
          expireText: expireText(c.endsAt),
          expired: isExpired(c.endsAt) || false,
          claimUserCount: c.claimUserCount || 0,
          unusedCount: c.unusedCount || 0,
        })),
      });
    } catch (e) {}
  },
  openSheet() {
    this.setData({ sheetOpen: true });
  },
  closeSheet() {
    if (!this.data.sheetOpen) return;
    this.setData({ sheetOpen: false });
  },
  noop() {},
  onTitle(e) {
    this.setData({ title: e.detail.value });
  },
  onFace(e) {
    this.setData({ faceYuan: e.detail.value });
  },
  onMin(e) {
    this.setData({ minSpendYuan: e.detail.value });
  },
  onExpire(e) {
    this.setData({ expireDate: e.detail.value || "" });
  },
  clearExpire() {
    this.setData({ expireDate: "" });
  },
  async toggle(e) {
    const id = e.currentTarget.dataset.id;
    const item = (this.data.list || []).find((c) => c.id === id);
    if (!item) return;
    if (item.enabled) {
      const n = item.claimUserCount || 0;
      const content = n
        ? copy.format(copy.merchantCouponForm.disableConfirm, { n })
        : copy.merchantCouponForm.disableConfirmNone;
      wx.showModal({
        title: item.title || copy.merchantCouponForm.disable,
        content,
        success: async (res) => {
          if (!res.confirm) return;
          await this.saveEnabled(item, false);
        },
      });
      return;
    }
    await this.saveEnabled(item, true);
  },
  async saveEnabled(item, enabled) {
    try {
      await api.merchantCouponUpdate(item.id, {
        title: item.title,
        type: item.type || "FIXED",
        faceFen: item.faceFen,
        minSpendFen: item.minSpendFen,
        totalLimit: item.totalLimit,
        perUserLimit: item.perUserLimit,
        startsAt: item.startsAt,
        endsAt: item.endsAt,
        enabled,
      });
      await this.reload();
    } catch (err) {}
  },
  remove(e) {
    const id = e.currentTarget.dataset.id;
    const title = e.currentTarget.dataset.title || "";
    const item = (this.data.list || []).find((c) => c.id === id);
    const n = item ? item.claimUserCount || 0 : 0;
    const unused = item ? item.unusedCount || 0 : 0;
    const content = n
      ? copy.format(copy.merchantCouponForm.removeConfirmUsers, { n, unused })
      : copy.merchantCouponForm.removeConfirm;
    wx.showModal({
      title: title || copy.merchantCouponForm.remove,
      content,
      success: async (res) => {
        if (!res.confirm) return;
        try {
          await api.merchantCouponDelete(id);
          wx.showToast({ title: copy.merchantCouponForm.removed, icon: "none" });
          await this.reload();
        } catch (err) {}
      },
    });
  },
  async create() {
    const title = (this.data.title || "").trim();
    const faceFen = Math.round(Number(this.data.faceYuan) * 100);
    const minSpendFen = Math.round(Number(this.data.minSpendYuan || 0) * 100);
    if (!title || !Number.isFinite(faceFen) || faceFen < 1) {
      wx.showToast({ title: "请填写名称和面额", icon: "none" });
      return;
    }
    try {
      await api.merchantCouponCreate({
        title,
        type: "FIXED",
        faceFen,
        minSpendFen,
        endsAt: this.data.expireDate ? this.data.expireDate + "T23:59:59" : null,
        enabled: true,
      });
      wx.showToast({ title: copy.merchantCouponForm.created, icon: "none" });
      this.setData({ title: "", faceYuan: "", minSpendYuan: "0", expireDate: "", sheetOpen: false });
      await this.reload();
    } catch (e) {}
  },
});
