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
      this.setData({
        claimList: (claimList || []).map((c) => this.mapCoupon(c)),
        mineList: (mineList || []).map((c) => this.mapMine(c)),
      });
    } catch (e) {}
  },
  mapCoupon(c) {
    const faceYuan = fenToYuan(c.faceFen);
    const minYuan = fenToYuan(c.minSpendFen);
    return {
      ...c,
      faceText: format(copy.coupons.face, { yuan: faceYuan }),
      minText: c.minSpendFen > 0 ? format(copy.coupons.minSpend, { yuan: minYuan }) : "",
    };
  },
  mapMine(c) {
    const faceYuan = fenToYuan(c.faceFen);
    return {
      ...c,
      faceText: format(copy.coupons.face, { yuan: faceYuan }),
    };
  },
  switchTab(e) {
    this.setData({ tab: e.currentTarget.dataset.tab });
  },
  async claim(e) {
    const id = e.currentTarget.dataset.id;
    try {
      await api.couponClaim(id);
      wx.showToast({ title: copy.coupons.claimed, icon: "none" });
      await this.reload();
    } catch (err) {}
  },
});
