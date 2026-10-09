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
    catIntoView: "cat-0",
    filterReco: "all",
    filterShelf: "all",
    filterStock: "all",
    emptyText: "暂无商品，点上方新增",
    selecting: false,
    selectedCount: 0,
    allVisibleSelected: false,
    selectedLabel: "",
  },
  onLoad() {
    this._selected = {};
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
    const { allList, searchKeyword, filterCategoryId, filterReco, filterShelf, filterStock } = this.data;
    const kw = (searchKeyword || "").trim().toLowerCase();
    const selected = this._selected || {};
    const alive = {};
    (allList || []).forEach((p) => {
      alive[p.id] = true;
    });
    Object.keys(selected).forEach((id) => {
      if (!alive[id] && !alive[Number(id)]) delete selected[id];
    });
    const list = (allList || []).filter((p) => {
      if (kw && !(p.name || "").toLowerCase().includes(kw)) return false;
      if (filterCategoryId && p.categoryId !== filterCategoryId) return false;
      if (filterReco === "on" && !p.homeRecommended) return false;
      if (filterReco === "off" && p.homeRecommended) return false;
      if (filterShelf === "on" && !p.onShelf) return false;
      if (filterShelf === "off" && p.onShelf) return false;
      if (filterStock === "out" && p.inStock) return false;
      return true;
    });
    const hasFilter = !!(
      kw ||
      filterCategoryId ||
      filterReco !== "all" ||
      filterShelf !== "all" ||
      filterStock !== "all"
    );
    const shown = list.map((p) => ({ ...p, selected: !!selected[p.id] }));
    const selectedCount = Object.keys(selected).length;
    const allVisibleSelected = shown.length > 0 && shown.every((p) => p.selected);
    this.setData({
      list: shown,
      selectedCount,
      allVisibleSelected,
      selectedLabel: copy.format(copy.merchant.batchSelected, { n: selectedCount }),
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
    this.setData({ filterCategoryId: id, catIntoView: "cat-" + id });
    this.applyFilter();
  },
  onFilterReco(e) {
    const filterReco = e.currentTarget.dataset.reco || "all";
    this.setData({ filterReco });
    this.applyFilter();
  },
  onFilterShelf(e) {
    const next = e.currentTarget.dataset.shelf || "all";
    this.setData({ filterShelf: this.data.filterShelf === next ? "all" : next });
    this.applyFilter();
  },
  onFilterStock() {
    this.setData({ filterStock: this.data.filterStock === "out" ? "all" : "out" });
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
  onCard(e) {
    if (this.data.selecting) {
      this.toggleOne(e.currentTarget.dataset.id);
      return;
    }
    this.edit(e);
  },
  toggleSelectMode() {
    if (this.data.selecting) {
      this._selected = {};
      this.setData({ selecting: false });
      this.applyFilter();
      return;
    }
    this._selected = this._selected || {};
    this.setData({ selecting: true });
    this.applyFilter();
  },
  toggleSelectAll() {
    const ids = (this.data.list || []).map((p) => p.id);
    if (!ids.length) return;
    this._selected = this._selected || {};
    const allOn = ids.every((id) => this._selected[id]);
    ids.forEach((id) => {
      if (allOn) delete this._selected[id];
      else this._selected[id] = true;
    });
    this.applyFilter();
  },
  toggleOne(id) {
    const key = Number(id);
    if (!key) return;
    this._selected = this._selected || {};
    if (this._selected[key]) delete this._selected[key];
    else this._selected[key] = true;
    this.applyFilter();
  },
  runBatch(e) {
    const op = e.currentTarget.dataset.op;
    const ids = Object.keys(this._selected || {}).map((id) => Number(id)).filter(Boolean);
    if (!ids.length) {
      wx.showToast({ title: copy.merchant.batchNeedPick, icon: "none" });
      return;
    }
    if (op === "DELETE") {
      const content = ids.length === 1
        ? copy.merchant.removeProductConfirm
        : copy.format(copy.merchant.removeProductsConfirm, { n: ids.length });
      wx.showModal({
        title: copy.merchant.removeProduct,
        content,
        confirmText: "删除",
        confirmColor: "#DC2626",
        success: (res) => {
          if (res.confirm) this.submitBatch(op, ids);
        },
      });
      return;
    }
    this.submitBatch(op, ids);
  },
  async submitBatch(op, ids) {
    if (this._batching) return;
    this._batching = true;
    try {
      await api.merchantProductBatch({ ids, op });
      ids.forEach((id) => delete this._selected[id]);
      wx.showToast({
        title: op === "DELETE" ? copy.merchant.removed : copy.merchant.batchUpdated,
        icon: "none",
      });
      await this.load();
    } catch (err) {
    } finally {
      this._batching = false;
    }
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
