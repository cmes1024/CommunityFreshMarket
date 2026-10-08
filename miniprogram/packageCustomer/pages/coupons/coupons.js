const api = require("../../../utils/api");
const copy = require("../../../utils/copy");
const { fenToYuan } = require("../../../utils/money");
const { format } = require("../../../utils/copy");

Page({
  data: {
    tab: "claim",
    claimList: [],
    mineList: [],
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
      this.setData({
        claimList: (claimList || []).map((c) => this.mapCoupon(c, mineCount[c.id] || 0)),
        mineList: mine,
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
    if (reached) {
      actionText = copy.coupons.claimed;
    } else if (soldOut) {
      actionText = copy.coupons.soldOut;
    }
    return {
      ...c,
      faceText: format(copy.coupons.face, { yuan: faceYuan }),
      minText: c.minSpendFen > 0 ? format(copy.coupons.minSpend, { yuan: minYuan }) : "",
      actionText,
      claimable: actionText === copy.coupons.claim,
    };
  },
  mapMine(c) {
    const faceYuan = fenToYuan(c.faceFen);
    const statusMap = {
      UNUSED: copy.coupons.statusUnused,
      RESERVED: copy.coupons.statusReserved,
      USED: copy.coupons.statusUsed,
      EXPIRED: copy.coupons.statusExpired,
    };
    return {
      ...c,
      faceText: format(copy.coupons.face, { yuan: faceYuan }),
      statusText: statusMap[c.status] || c.status,
    };
  },
  switchTab(e) {
    this.setData({ tab: e.currentTarget.dataset.tab });
  },
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
