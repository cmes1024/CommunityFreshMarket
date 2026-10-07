const api = require("../../../utils/api");
const { fenToYuan } = require("../../../utils/money");
const { productCover } = require("../../../utils/productImages");
const copy = require("../../../utils/copy");

function mapProduct(p) {
  return {
    ...p,
    priceYuan: fenToYuan(p.priceFen),
    coverUrl: productCover(p),
  };
}

Page({
  data: {
    allList: [],
    list: [],
    cats: [],
    searchKeyword: "",
    filterCategoryId: 0,
    filterReco: "all",
    emptyText: "暂无商品，点上方新增",
  },
  async onShow() {
    await this.load();
  },
  async load() {
    const [products, cats] = await Promise.all([
      api.merchantProducts(),
      api.merchantCategories(),
    ]);
    this.setData({
      allList: (products || []).map(mapProduct),
      cats: (cats || []).filter((c) => c.enabled),
    });
    this.applyFilter();
  },
  patchLocal(updated) {
    if (!updated || !updated.id) return;
    const mapped = mapProduct(updated);
    const allList = (this.data.allList || []).map((p) =>
      p.id === mapped.id ? { ...p, ...mapped } : p
    );
    this.setData({ allList });
    this.applyFilter();
  },
  applyFilter() {
    const { allList, searchKeyword, filterCategoryId, filterReco } = this.data;
    const kw = (searchKeyword || "").trim().toLowerCase();
    const list = (allList || []).filter((p) => {
      if (kw && !(p.name || "").toLowerCase().includes(kw)) return false;
      if (filterCategoryId && p.categoryId !== filterCategoryId) return false;
      if (filterReco === "on" && !p.homeRecommended) return false;
      if (filterReco === "off" && p.homeRecommended) return false;
      return true;
    });
    const hasFilter = !!(kw || filterCategoryId || filterReco !== "all");
    this.setData({
      list,
      emptyText: hasFilter
        ? copy.merchant.filterEmpty
        : "暂无商品，点上方新增",
    });
  },
  onSearch(e) {
    const searchKeyword = e.detail.value || "";
    this.setData({ searchKeyword });
    if (this._searchTimer) clearTimeout(this._searchTimer);
    this._searchTimer = setTimeout(() => this.applyFilter(), 280);
  },
  clearSearch() {
    this.setData({ searchKeyword: "" });
    this.applyFilter();
  },
  onFilterCat(e) {
    const id = Number(e.currentTarget.dataset.id || 0);
    this.setData({ filterCategoryId: id });
    this.applyFilter();
  },
  onFilterReco(e) {
    const filterReco = e.currentTarget.dataset.reco || "all";
    this.setData({ filterReco });
    this.applyFilter();
  },
  goCats() {
    wx.navigateTo({ url: "/packageMerchant/pages/categories/categories" });
  },
  edit(e) {
    wx.navigateTo({
      url: `/packageMerchant/pages/product-edit/product-edit?id=${e.currentTarget.dataset.id}`,
    });
  },
  create() {
    wx.navigateTo({ url: "/packageMerchant/pages/product-edit/product-edit" });
  },
  async toggleShelf(e) {
    const { id, on } = e.currentTarget.dataset;
    try {
      const updated = await api.merchantOnShelf(id, !(on === 1 || on === "1"));
      this.patchLocal(updated);
    } catch (err) {}
  },
  async toggleStock(e) {
    const { id, on } = e.currentTarget.dataset;
    try {
      const updated = await api.merchantInStock(id, !(on === 1 || on === "1"));
      this.patchLocal(updated);
    } catch (err) {}
  },
  async toggleHomeReco(e) {
    const { id, on } = e.currentTarget.dataset;
    try {
      const updated = await api.merchantHomeRecommended(
        id,
        !(on === 1 || on === "1")
      );
      this.patchLocal(updated);
    } catch (err) {}
  },
});
