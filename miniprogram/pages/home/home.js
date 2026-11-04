const api = require("../../utils/api");
const { fenToYuan, yuanPlain } = require("../../utils/money");
const { syncCartCount, qtyMap, lastCart, setQty } = require("../../utils/cartSync");
const copy = require("../../utils/copy");
const { listUnitLine } = require("../../utils/productUnitLine");
const { openProduct } = require("../../utils/productOpen");
const { qtyBounds, normalizeCartQty } = require("../../utils/productQty");
const { goTab } = require("../../utils/nav");
const { storeDisplayName } = copy;

function heroImagesOf(store) {
  const list = store && store.heroImageUrls;
  if (Array.isArray(list) && list.length) {
    return list.map((url) => String(url || "").trim()).filter(Boolean).slice(0, 3);
  }
  const one = store && store.heroImageUrl;
  return one ? [one] : [];
}

/** 公告滚动一遍的时长，交给 CSS 动画，不再自己开定时器 */
function noticeDurationMs(text) {
  const n = String(text || "").trim().length;
  return Math.max(8, Math.round(n * 0.45)) * 1000;
}

function normalizeCols(cols) {
  const n = Number(cols);
  return n === 1 || n === 3 ? n : 2;
}

function deliverySubText(store) {
  const min = Number(store && store.minDeliveryFen) || 0;
  if (min > 0) return copy.format(copy.home.deliveryMin, { yuan: yuanPlain(min) });
  return copy.home.deliverySub;
}

/** 由数据算出一屏所需的全部字段：首帧预热与正常加载共用同一份逻辑 */
function buildHomeView(store, categories, products, slots, cart) {
  const qtys = qtyMap(cart);
  const notice = (store && store.homeNotice) || "";
  const ranked = (products || []).map((p) => {
    const bounds = qtyBounds(p, { forDetail: false });
    const minAdd = bounds.min > 0 ? bounds.min : 1;
    return {
      ...p,
      priceYuan: fenToYuan(p.priceFen),
      unitLine: listUnitLine(p),
      qty: qtys[p.id] || 0,
      qtyMax: bounds.max,
      qtyMinAdd: minAdd,
    };
  });
  return {
    store: store || {},
    displayStoreName: storeDisplayName(store),
    homeKicker: (store && store.homeKicker) || copy.home.kicker,
    // 主标题与导航店名分工：有配置才显示，空则不显示，避免重复
    homeTitle: (store && store.homeTitle) || "",
    homeSubtitle: (store && store.homeSubtitle) || copy.home.heroDesc,
    homeNotice: notice,
    noticeDurationMs: noticeDurationMs(notice),
    heroImages: heroImagesOf(store),
    homeProductCols: normalizeCols(store && store.homeProductCols),
    categories: categories || [],
    deliverySub: deliverySubText(store),
    pickupHint: (slots && slots[0] && slots[0].label) || copy.home.pickupSubFallback,
    products: ranked,
  };
}

Page({
  data: {
    store: {},
    displayStoreName: copy.storeName,
    homeKicker: copy.home.kicker,
    homeTitle: "",
    homeSubtitle: copy.home.heroDesc,
    homeNotice: "",
    noticeDurationMs: 12000,
    heroImages: [],
    homeProductCols: 2,
    categories: [],
    products: [],
    pickupHint: "",
    deliverySub: copy.home.deliverySub,
    sheetShow: false,
    sheetProductId: 0,
    sheetPreview: null,
  },
  onLoad() {
    // 用上一次的数据把首帧填满，切 tab 不再先白屏
    const store = api.peekStore();
    const categories = api.peekCategories();
    const products = api.peekProducts({ homeRecommended: true });
    const slots = api.peekPickupSlots();
    this._refs = { store, categories, products, slots };
    this.setData(buildHomeView(store, categories, products, slots, lastCart()));
  },
  onShow() {
    this.load();
  },
  async load() {
    try {
      const [store, categories, products, slots] = await Promise.all([
        api.store(),
        api.categories(),
        api.products({ homeRecommended: true }),
        api.pickupSlots().catch(() => []),
      ]);
      // 缓存命中时引用不变，说明一屏内容没变，不必再推一次大数据
      const prev = this._refs || {};
      const same =
        prev.store === store &&
        prev.categories === categories &&
        prev.products === products &&
        prev.slots === slots;
      this._refs = { store, categories, products, slots };
      if (!same) this.setData(buildHomeView(store, categories, products, slots, lastCart()));
      syncCartCount().then((cart) => this.applyQty(cart));
    } catch (e) {
      this.setData({ displayStoreName: storeDisplayName(null) });
    }
  },
  /** 只把数量有变化的卡片刷一遍，避免整表重渲染 */
  applyQty(cart) {
    if (!cart) return;
    const qtys = qtyMap(cart);
    const patch = {};
    (this.data.products || []).forEach((p, i) => {
      const next = qtys[p.id] || 0;
      if (p.qty !== next) patch[`products[${i}].qty`] = next;
    });
    if (Object.keys(patch).length) this.setData(patch);
  },
  goAll() {
    goTab("/pages/category/category");
  },
  goCategory(e) {
    const id = e.currentTarget.dataset.id;
    // switchTab 不能带参数，选中哪个分类用 globalData 传
    getApp().globalData.pendingCategoryId = id;
    goTab("/pages/category/category");
  },
  goDetail(e) {
    const pid = Number(e.currentTarget.dataset.id);
    const preview = (this.data.products || []).find((p) => p.id === pid) || null;
    openProduct(this, pid, this.data.store, preview);
  },
  onSheetClose() {
    if (this.data.sheetShow) this.setData({ sheetShow: false });
  },
  onSheetChanged(e) {
    const cart = e && e.detail && e.detail.cart;
    if (cart) {
      this.applyQty(cart);
      return;
    }
    syncCartCount().then((next) => this.applyQty(next));
  },
  noop() {},
  async onQty(e) {
    const id = e.currentTarget.dataset.id;
    const minAdd = Number(e.currentTarget.dataset.min) || 1;
    const max = Number(e.currentTarget.dataset.max) || 999;
    let qty = normalizeCartQty(e.detail.value, minAdd);
    qty = Math.min(max, qty);
    const idx = (this.data.products || []).findIndex((p) => p.id === id);
    if (idx >= 0) {
      if (this.data.products[idx].qty === qty) return;
      // 先动界面，网络在后台同步
      this.setData({ [`products[${idx}].qty`]: qty });
    }
    try {
      const cart = await setQty(id, qty);
      this.applyQty(cart);
    } catch (err) {
      syncCartCount().then((cart) => this.applyQty(cart));
    }
  },
});
