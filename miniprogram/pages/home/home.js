const api = require("../../utils/api");
const { fenToYuan, yuanPlain } = require("../../utils/money");
const { syncCartCount, qtyMap, lastCart, setQty } = require("../../utils/cartSync");
const copy = require("../../utils/copy");
const { listUnitLine } = require("../../utils/productUnitLine");
const { openProduct } = require("../../utils/productOpen");
const { qtyBounds, normalizeCartQty } = require("../../utils/productQty");
const { goTab } = require("../../utils/nav");
const { syncTabBar } = require("../../utils/tabbar");
const hud = require("../../utils/hud");
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
    /* 头图轮播开关：页面被盖住时停掉，避免和上一个页面的轮播叠着跑 */
    heroAutoplay: true,
    /* 公告跑马灯：infinite 动画在页面被盖住后仍会在同一渲染层里一直跑 */
    noticeRunning: true,
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
    syncTabBar(this, "home");
    // 从详情页等页面返回时把头图轮播续上
    const patch = {};
    if (!this.data.heroAutoplay) patch.heroAutoplay = true;
    if (!this.data.noticeRunning) patch.noticeRunning = true;
    if (Object.keys(patch).length) this.setData(patch);
    this.startHud();
    this.load();
  },
  onHide() {
    // swiper 的 autoplay 是视图层定时器，页面被 navigateTo 盖住后不会自己停：
    // 首页 2.5s 一轮 + 整页详情 3s 一轮同时跑，低端机上就是持续掉帧
    const patch = {};
    if (this.data.heroAutoplay) patch.heroAutoplay = false;
    // 公告跑马灯同理，CSS infinite 动画盖住后也一直在同一渲染层重绘
    if (this.data.noticeRunning) patch.noticeRunning = false;
    if (Object.keys(patch).length) this.setData(patch);
    this.stopHud();
  },
  onUnload() {
    this.stopHud();
  },
  /** 真机取证面板：默认关闭（utils/hud.js 里的 ENABLED），只在首页可见时跑 */
  startHud() {
    if (!hud.enabled) return;
    this.stopHud();
    const tick = () => this.setData({ hud: hud.sample() });
    tick();
    this._hudTimer = setInterval(tick, 1000);
  },
  stopHud() {
    if (this._hudTimer) {
      clearInterval(this._hudTimer);
      this._hudTimer = null;
    }
    if (this.data.hud) this.setData({ hud: null });
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
  /** 卡片上的内联加减：不再为每张卡建 Stepper 组件实例 */
  stepQty(e) {
    const ds = e.currentTarget.dataset;
    const id = ds.id;
    const delta = Number(ds.delta) || 1;
    const item = (this.data.products || []).find((p) => p.id === id);
    if (!item) return;
    const minAdd = Number(item.qtyMinAdd) || 1;
    const max = Number(item.qtyMax) || 999;
    const step = item.qty > 0 ? 1 : minAdd;
    const next = Math.max(0, Math.min(max, Number((item.qty + delta * step).toFixed(2))));
    if (next === item.qty) return;
    this.onQty({
      currentTarget: { dataset: { id, min: minAdd, max } },
      detail: { value: next },
    });
  },
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
