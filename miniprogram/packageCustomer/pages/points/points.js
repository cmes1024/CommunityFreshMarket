const api = require("../../../utils/api");
const copy = require("../../../utils/copy");
const { format } = require("../../../utils/copy");

function reasonText(reason) {
  const map = {
    ORDER_COMPLETE: copy.points.reasonOrder,
    CASHIER_COMPLETE: copy.points.reasonCashier,
    REDEEM: copy.points.reasonRedeem,
    ADJUST: copy.points.reasonAdjust,
    CANCEL_REVERSAL: copy.points.reasonReverse,
  };
  return map[reason] || reason || "";
}

function redeemStatus(status) {
  const map = {
    PENDING: copy.points.statusPending,
    DONE: copy.points.statusDone,
    CANCELLED: copy.points.statusCancelled,
  };
  return map[status] || status || "";
}

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
    items: [],
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
          reasonText: reasonText(r.reason),
        })),
        redeems: (me.redeems || []).map((r) => ({
          ...r,
          timeText: fmtTime(r.createdAt),
          statusText: redeemStatus(r.status),
        })),
        items: (me.items || []).map((item) => ({
          ...item,
          affordable: (me.balance || 0) >= item.costPoints,
        })),
      });
    } catch (e) {}
  },
  redeemItem(e) {
    const id = e.currentTarget.dataset.id;
    const item = (this.data.items || []).find((it) => it.id === id);
    if (!item) return;
    if (!item.affordable) {
      wx.showToast({ title: copy.points.notEnough, icon: "none" });
      return;
    }
    wx.showModal({
      title: item.title,
      content: `使用 ${item.costPoints} 积分。不生成预约单，到店后由店员核销`,
      success: async (res) => {
        if (!res.confirm) return;
        try {
          await api.pointsRedeem({ itemId: id });
          wx.showToast({ title: copy.points.redeemOk, icon: "none" });
          await this.reload();
        } catch (err) {}
      },
    });
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
