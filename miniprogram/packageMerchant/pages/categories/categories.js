const api = require("../../../utils/api");

Page({
  data: { list: [], name: "", adding: false },
  async onShow() {
    await this.reload();
  },
  async reload() {
    this.setData({ list: await api.merchantCategories() });
  },
  onName(e) {
    this.setData({ name: e.detail.value });
  },
  showAdd() {
    this.setData({ adding: true });
  },
  async add() {
    const name = (this.data.name || "").trim();
    if (!name) return;
    try {
      await api.merchantCategoryCreate({ name, enabled: true });
      this.setData({ name: "", adding: false });
      await this.reload();
    } catch (e) {}
  },
  async toggle(e) {
    const { id, on, name } = e.currentTarget.dataset;
    const enabled = on === 1 || on === "1";
    try {
      await api.merchantCategoryUpdate(id, { name, enabled: !enabled });
      await this.reload();
    } catch (err) {}
  },
  async remove(e) {
    try {
      await api.merchantCategoryDelete(e.currentTarget.dataset.id);
      await this.reload();
    } catch (err) {}
  },
});
