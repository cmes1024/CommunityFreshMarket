const api = require("../../../utils/api");
const copy = require("../../../utils/copy");

function fmtTime(iso) {
  if (!iso) return "";
  const s = String(iso).replace("T", " ");
  return s.length > 16 ? s.slice(0, 16) : s;
}

function matchProducts(products, keyword) {
  const kw = (keyword || "").trim().toLowerCase();
  return (products || []).filter((p) => !kw || String(p.name || "").toLowerCase().includes(kw));
}

Page({
  data: {
    list: [],
    items: [],
    products: [],
    filtered: [],
    productId: 0,
    productName: "",
    title: "",
    cost: "",
    sheet: "",
    keyword: "",
  },
  async onShow() {
    await this.reload();
  },
  async reload() {
    try {
      const [list, items, products] = await Promise.all([
        api.merchantPointRedeems(),
        api.merchantPointItems(),
        api.merchantProducts(),
      ]);
      const shelf = (products || []).filter((p) => p.onShelf !== false);
      this.setData({
        list: (list || []).map((r) => ({
          ...r,
          timeText: fmtTime(r.createdAt),
        })),
        items: items || [],
        products: shelf,
        filtered: matchProducts(shelf, this.data.keyword),
      });
    } catch (e) {}
  },
  openPicker() {
    this.setData({
      sheet: "picker",
      keyword: "",
      filtered: this.data.products,
    });
  },
  openPending() {
    this.setData({ sheet: "pending" });
  },
  closeSheet() {
    if (!this.data.sheet) return;
    this.setData({ sheet: "", keyword: "" });
  },
  noop() {},
  onKeyword(e) {
    const keyword = e.detail.value || "";
    this.setData({
      keyword,
      filtered: matchProducts(this.data.products, keyword),
    });
  },
  clearKeyword() {
    this.setData({ keyword: "", filtered: this.data.products });
  },
  selectProduct(e) {
    const id = Number(e.currentTarget.dataset.id) || 0;
    const product = (this.data.products || []).find((p) => p.id === id);
    this.setData({
      productId: id,
      productName: product ? product.name : "",
      title: product ? product.name : this.data.title,
      sheet: "",
      keyword: "",
    });
  },
  clearProduct() {
    this.setData({ productId: 0, productName: "" });
  },
  onTitle(e) {
    this.setData({ title: e.detail.value });
  },
  onCost(e) {
    this.setData({ cost: e.detail.value });
  },
  async addItem() {
    const title = (this.data.title || "").trim();
    const costPoints = Number(this.data.cost);
    const productId = this.data.productId || null;
    if ((!title && !productId) || !Number.isFinite(costPoints) || costPoints < 1) {
      wx.showToast({ title: "请选择商品或填写名称，并填写积分", icon: "none" });
      return;
    }
    try {
      await api.merchantPointItemCreate({
        productId,
        title,
        costPoints,
        enabled: true,
      });
      wx.showToast({ title: copy.merchantPointRedeem.added, icon: "none" });
      this.setData({ title: "", cost: "", productId: 0, productName: "" });
      await this.reload();
    } catch (err) {}
  },
  async toggleItem(e) {
    const id = e.currentTarget.dataset.id;
    const item = (this.data.items || []).find((it) => it.id === id);
    if (!item) return;
    try {
      await api.merchantPointItemUpdate(id, {
        productId: item.productId || null,
        title: item.title,
        costPoints: item.costPoints,
        enabled: !item.enabled,
      });
      await this.reload();
    } catch (err) {}
  },
  removeItem(e) {
    const id = e.currentTarget.dataset.id;
    const title = e.currentTarget.dataset.title || "";
    wx.showModal({
      title: title || copy.merchantPointRedeem.remove,
      content: copy.merchantPointRedeem.removeConfirm,
      success: async (res) => {
        if (!res.confirm) return;
        try {
          await api.merchantPointItemDelete(id);
          wx.showToast({ title: copy.merchantPointRedeem.removed, icon: "none" });
          await this.reload();
        } catch (err) {}
      },
    });
  },
  async reject(e) {
    const id = e.currentTarget.dataset.id;
    try {
      await api.merchantPointRedeemCancel(id);
      wx.showToast({ title: copy.merchantPointRedeem.rejected, icon: "none" });
      await this.reload();
    } catch (err) {}
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
