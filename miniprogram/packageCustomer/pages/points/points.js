const api = require("../../../utils/api");
const copy = require("../../../utils/copy");
const { format } = require("../../../utils/copy");

function fmtTime(iso) {
  if (!iso) return "";
  const s = String(iso).replace("T", " ");
  return s.length > 16 ? s.slice(0, 16) : s;
}

Page({
  data: {
    balance: 0,
    pointsPerYuan: 0,
    perYuanText: "",
    recent: [],
    redeems: [],
    redeemTitle: "",
    redeemCost: "",
  },
  async onShow() {
    await this.reload();
  },
  async reload() {
    try {
      const me = await api.pointsMe();
      const pointsPerYuan = me.pointsPerYuan || 0;
      this.setData({
        balance: me.balance || 0,
        pointsPerYuan,
        perYuanText: format(copy.points.perYuan, { n: pointsPerYuan }),
        recent: (me.recent || []).map((r) => ({
          ...r,
          timeText: fmtTime(r.createdAt),
          deltaText: r.delta > 0 ? `+${r.delta}` : String(r.delta),
        })),
        redeems: (me.redeems || []).map((r) => ({
          ...r,
          timeText: fmtTime(r.createdAt),
        })),
      });
    } catch (e) {}
  },
  onRedeemTitle(e) {
    this.setData({ redeemTitle: e.detail.value });
  },
  onRedeemCost(e) {
    this.setData({ redeemCost: e.detail.value });
  },
  async submitRedeem() {
    const title = (this.data.redeemTitle || "").trim();
    const costPoints = Number(this.data.redeemCost);
    if (!title || !Number.isFinite(costPoints) || costPoints < 1) {
      wx.showToast({ title: "请填写说明和积分", icon: "none" });
      return;
    }
    try {
      await api.pointsRedeem({ title, costPoints });
      wx.showToast({ title: copy.points.redeemOk, icon: "none" });
      this.setData({ redeemTitle: "", redeemCost: "" });
      await this.reload();
    } catch (e) {}
  },
  async doCheckin() {
    let body = {};
    try {
      const loc = await new Promise((resolve, reject) => {
        wx.getLocation({ type: "gcj02", success: resolve, fail: reject });
      });
      body = { lat: loc.latitude, lng: loc.longitude };
    } catch (e) {}
    try {
      await api.checkin(body);
      wx.showToast({ title: copy.profile.checkinDone, icon: "none" });
    } catch (e) {
      wx.showToast({ title: copy.profile.checkinFail, icon: "none" });
    }
  },
});
