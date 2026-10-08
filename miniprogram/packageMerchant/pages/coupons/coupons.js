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

function claimWindowText(startsAt, claimEndsAt) {
  const start = expireText(startsAt);
  const end = expireText(claimEndsAt);
  if (start && end) return "领 " + start + " 至 " + end;
  if (start) return "领自 " + start;
  if (end) return "领至 " + end;
  return "";
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
    startsDate: "",
    claimEndDate: "",
    totalLimit: "",
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
          claimWindowText: claimWindowText(c.startsAt, c.claimEndsAt),
          limitText: c.totalLimit ? "限 " + c.totalLimit + " 张" : "",
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
  onStart(e) {
    this.setData({ startsDate: e.detail.value || "" });
  },
  onClaimEnd(e) {
    this.setData({ claimEndDate: e.detail.value || "" });
  },
  onTotal(e) {
    this.setData({ totalLimit: e.detail.value });
  },
  clearExpire() {
    this.setData({ expireDate: "" });
  },
  clearStart() {
    this.setData({ startsDate: "" });
  },
  clearClaimEnd() {
    this.setData({ claimEndDate: "" });
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
        claimEndsAt: item.claimEndsAt,
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
    const totalText = String(this.data.totalLimit || "").trim();
    let totalLimit = null;
    if (totalText) {
      if (!/^\d+$/.test(totalText) || Number(totalText) < 1) {
        wx.showToast({ title: copy.merchantCouponForm.totalLimitInvalid, icon: "none" });
        return;
      }
      totalLimit = Number(totalText);
    }
    if (this.data.startsDate && this.data.claimEndDate && this.data.startsDate > this.data.claimEndDate) {
      wx.showToast({ title: copy.merchantCouponForm.startAfterClaimEnd, icon: "none" });
      return;
    }
    if (this.data.startsDate && this.data.expireDate && this.data.startsDate > this.data.expireDate) {
      wx.showToast({ title: copy.merchantCouponForm.startAfterEnd, icon: "none" });
      return;
    }
    if (this.data.claimEndDate && this.data.expireDate && this.data.claimEndDate > this.data.expireDate) {
      wx.showToast({ title: copy.merchantCouponForm.claimEndAfterUse, icon: "none" });
      return;
    }
    try {
      await api.merchantCouponCreate({
        title,
        type: "FIXED",
        faceFen,
        minSpendFen,
        totalLimit,
        startsAt: this.data.startsDate ? this.data.startsDate + "T00:00:00" : null,
        claimEndsAt: this.data.claimEndDate ? this.data.claimEndDate + "T23:59:59" : null,
        endsAt: this.data.expireDate ? this.data.expireDate + "T23:59:59" : null,
        enabled: true,
      });
      wx.showToast({ title: copy.merchantCouponForm.created, icon: "none" });
      this.setData({
        title: "",
        faceYuan: "",
        minSpendYuan: "0",
        expireDate: "",
        startsDate: "",
        claimEndDate: "",
        totalLimit: "",
        sheetOpen: false,
      });
      await this.reload();
    } catch (e) {}
  },
});
