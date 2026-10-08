const api = require("../../../utils/api");
const copy = require("../../../utils/copy");

function fmtTime(iso) {
  if (!iso) return "";
  const s = String(iso).replace("T", " ");
  return s.length > 16 ? s.slice(0, 16) : s;
}

Page({
  data: { list: [] },
  async onShow() {
    await this.reload();
  },
  async reload() {
    try {
      const list = await api.merchantPointRedeems();
      this.setData({
        list: (list || []).map((r) => ({
          ...r,
          timeText: fmtTime(r.createdAt),
        })),
      });
    } catch (e) {}
  },
  async complete(e) {
    const id = e.currentTarget.dataset.id;
    try {
      await api.merchantPointRedeemComplete(id);
      wx.showToast({ title: copy.merchantPointRedeem.complete, icon: "none" });
      await this.reload();
    } catch (err) {}
  },
});
