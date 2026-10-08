const api = require("../../../utils/api");
const copy = require("../../../utils/copy");
const { fenToYuan } = require("../../../utils/money");

Page({
  data: {
    title: "",
    faceYuan: "",
    minSpendYuan: "0",
    list: [],
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
        })),
      });
    } catch (e) {}
  },
  onTitle(e) {
    this.setData({ title: e.detail.value });
  },
  onFace(e) {
    this.setData({ faceYuan: e.detail.value });
  },
  onMin(e) {
    this.setData({ minSpendYuan: e.detail.value });
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
        type: "CASH",
        faceFen,
        minSpendFen,
        enabled: true,
      });
      wx.showToast({ title: copy.merchantCouponForm.created, icon: "none" });
      this.setData({ title: "", faceYuan: "", minSpendYuan: "0" });
      await this.reload();
    } catch (e) {}
  },
});
