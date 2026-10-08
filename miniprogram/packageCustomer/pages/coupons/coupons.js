const api = require("../../../utils/api");
const copy = require("../../../utils/copy");
const { fenToYuan } = require("../../../utils/money");
const { format } = require("../../../utils/copy");

Page({
  data: {
    tab: "claim",
    historySheet: false,
    claimList: [],
    mineList: [],
    historyList: [],
  },
  async onShow() {
    await this.reload();
  },
  async reload() {
    try {
      const [claimList, mineList] = await Promise.all([api.coupons(), api.couponsMine()]);
      const mine = (mineList || []).map((c) => this.mapMine(c));
      const mineCount = {};
      mine.forEach((c) => {
        mineCount[c.couponId] = (mineCount[c.couponId] || 0) + 1;
      });
      const available = mine.filter((c) => c.status === "UNUSED");
      const inUse = mine.filter((c) => c.status === "RESERVED");
      this.setData({
        claimList: (claimList || []).map((c) => this.mapCoupon(c, mineCount[c.id] || 0)),
        mineList: available.concat(inUse),
        historyList: mine.filter((c) => c.status !== "UNUSED" && c.status !== "RESERVED"),
      });
    } catch (e) {}
  },
  mapCoupon(c, mineCount) {
    const faceYuan = fenToYuan(c.faceFen);
    const minYuan = fenToYuan(c.minSpendFen);
    const perUser = c.perUserLimit == null ? 1 : c.perUserLimit;
    const reached = mineCount >= perUser;
    const soldOut = c.totalLimit != null && (c.claimedCount || 0) >= c.totalLimit;
    let actionText = copy.coupons.claim;
    let state = "";
    if (reached) {
      actionText = copy.coupons.claimed;
      state = "claimed";
    } else if (soldOut) {
      actionText = copy.coupons.soldOut;
      state = "soldout";
    }
    return {
      ...c,
      faceText: format(copy.coupons.face, { yuan: faceYuan }),
      minText: c.minSpendFen > 0 ? format(copy.coupons.minSpend, { yuan: minYuan }) : "",
      actionText,
      state,
      claimable: !state,
    };
  },
  mapMine(c) {
    const faceYuan = fenToYuan(c.faceFen);
    const minYuan = fenToYuan(c.minSpendFen);
    const historyMap = {
      USED: copy.coupons.statusUsed,
      EXPIRED: copy.coupons.statusExpired,
    };
    return {
      ...c,
      faceText: format(copy.coupons.face, { yuan: faceYuan }),
      minText: c.minSpendFen > 0 ? format(copy.coupons.minSpend, { yuan: minYuan }) : "",
      inUse: c.status === "RESERVED",
      historyText: historyMap[c.status] || copy.coupons.statusInvalid,
    };
  },
  switchTab(e) {
    this.setData({ tab: e.currentTarget.dataset.tab });
  },
  openHistory() {
    this.setData({ historySheet: true });
  },
  closeHistory() {
    if (!this.data.historySheet) return;
    this.setData({ historySheet: false });
  },
  noop() {},
  async claim(e) {
    const id = e.currentTarget.dataset.id;
    const item = (this.data.claimList || []).find((c) => String(c.id) === String(id));
    if (!item || !item.claimable) return;
    try {
      await api.couponClaim(id);
      wx.showToast({ title: copy.coupons.claimed, icon: "none" });
      await this.reload();
    } catch (err) {}
  },
});
