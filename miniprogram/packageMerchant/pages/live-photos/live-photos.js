const api = require("../../../utils/api");

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
      const list = await api.merchantLivePhotoRequests();
      this.setData({
        list: (list || []).map((r) => ({
          ...r,
          timeText: fmtTime(r.lastRequestedAt),
        })),
      });
    } catch (e) {}
  },
  open(e) {
    const id = e.currentTarget.dataset.id;
    if (!id) return;
    wx.navigateTo({ url: `/packageMerchant/pages/product-edit/product-edit?id=${id}` });
  },
});
