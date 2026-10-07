const api = require("../../../utils/api");
const copy = require("../../../utils/copy");
const { fenToYuan } = require("../../../utils/money");
const { chooseAndUpload } = require("../../../utils/upload");

function specPlaceholderFor(unit) {
  return unit === "JIN"
    ? copy.merchant.fieldSpecPlaceholderJin
    : copy.merchant.fieldSpecPlaceholderPiece;
}

Page({
  data: {
    id: 0,
    form: {
      name: "",
      categoryId: 0,
      unit: "JIN",
      specNote: "",
      priceFen: 0,
      imageUrl: "",
      inStock: true,
      onShelf: true,
    },
    priceYuan: "",
    units: [],
    unitLabels: [],
    unitIndex: 0,
    cats: [],
    catLabels: [],
    catIndex: 0,
    specPlaceholder: copy.merchant.fieldSpecPlaceholderJin,
  },
  async onLoad(query) {
    this._skipShow = true;
    this.id = query.id ? Number(query.id) : 0;
    this.setData({ id: this.id });
    await this.reloadMeta();
    if (this.id) {
      const p = await api.merchantProduct(this.id);
      const { units, cats } = this.data;
      const unitIndex = Math.max(0, units.findIndex((u) => u.code === p.unit));
      const catIndex = Math.max(0, cats.findIndex((c) => c.id === p.categoryId));
      this.setData({
        form: {
          name: p.name,
          categoryId: p.categoryId,
          unit: p.unit,
          specNote: p.specNote || "",
          priceFen: p.priceFen,
          imageUrl: p.imageUrl || "",
          inStock: p.inStock,
          onShelf: p.onShelf,
        },
        priceYuan: fenToYuan(p.priceFen),
        unitIndex,
        catIndex,
        specPlaceholder: specPlaceholderFor(p.unit),
      });
    }
  },
  async onShow() {
    if (this._skipShow) {
      this._skipShow = false;
      return;
    }
    await this.reloadMeta(true);
  },
  async reloadMeta(preserveSelection) {
    const [units, cats] = await Promise.all([api.units(), api.merchantCategories()]);
    const enabledCats = (cats || []).filter((c) => c.enabled);
    const patch = {
      units,
      unitLabels: units.map((u) => u.label),
      cats: enabledCats,
      catLabels: enabledCats.map((c) => c.name),
    };
    if (!preserveSelection && !this.id && enabledCats[0]) {
      const unitCode = (units[0] && units[0].code) || "JIN";
      patch["form.categoryId"] = enabledCats[0].id;
      patch["form.unit"] = unitCode;
      patch.unitIndex = 0;
      patch.catIndex = 0;
      patch.specPlaceholder = specPlaceholderFor(unitCode);
    } else if (preserveSelection && this.data.form.categoryId) {
      const catIndex = Math.max(
        0,
        enabledCats.findIndex((c) => c.id === this.data.form.categoryId)
      );
      patch.catIndex = catIndex;
      if (enabledCats[catIndex]) {
        patch["form.categoryId"] = enabledCats[catIndex].id;
      }
    }
    this.setData(patch);
  },
  goCats() {
    wx.navigateTo({ url: "/packageMerchant/pages/categories/categories" });
  },
  async pickImage() {
    try {
      const url = await chooseAndUpload();
      this.setData({ "form.imageUrl": url });
    } catch (e) {}
  },
  onName(e) {
    this.setData({ "form.name": e.detail.value });
  },
  onSpec(e) {
    this.setData({ "form.specNote": e.detail.value });
  },
  onPrice(e) {
    const priceYuan = e.detail.value;
    const priceFen = Math.round(Number(priceYuan) * 100);
    this.setData({ priceYuan, "form.priceFen": priceFen });
  },
  onUnit(e) {
    const unitIndex = Number(e.detail.value);
    const unit = this.data.units[unitIndex].code;
    this.setData({
      unitIndex,
      "form.unit": unit,
      specPlaceholder: specPlaceholderFor(unit),
    });
  },
  onCat(e) {
    const catIndex = Number(e.detail.value);
    this.setData({ catIndex, "form.categoryId": this.data.cats[catIndex].id });
  },
  async save() {
    const { form, id } = this.data;
    if (!form.name || form.priceFen < 0 || !form.categoryId) {
      wx.showToast({ title: "请填写名称、价格和分类", icon: "none" });
      return;
    }
    if (id) await api.merchantProductUpdate(id, form);
    else await api.merchantProductCreate(form);
    wx.navigateBack();
  },
});
