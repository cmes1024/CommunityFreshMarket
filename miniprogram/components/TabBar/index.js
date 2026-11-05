const api = require("../../utils/api");
const { onCartCountChange } = require("../../utils/cartSync");
const { memberCodeEntry } = require("../../utils/memberCodeEntry");
const merchantBadge = require("../../utils/merchantBadge");
const themeApply = require("../../utils/themeApply");
const { goTab } = require("../../utils/nav");
const { KEY_BY_ROUTE } = require("../../utils/tabbar");

/** 商家端底栏角标轮询间隔：站内信 + 冒泡都是轻接口 */
const MERCHANT_BADGE_INTERVAL = 15000;

const MEMBER_CODE_TAB = {
  key: "memberCode",
  label: "会员码",
  url: "/packageCustomer/pages/member-code/member-code",
  icon: "/images/tab-code.svg",
  iconActive: "/images/tab-code-active.svg",
  open: "push",
};

const CUSTOMER = [
  {
    key: "home",
    label: "首页",
    url: "/pages/home/home",
    icon: "/images/tab-home.svg",
    iconActive: "/images/tab-home-active.svg",
    open: "switchTab",
  },
  {
    key: "category",
    label: "分类",
    url: "/pages/category/category",
    icon: "/images/tab-grid.svg",
    iconActive: "/images/tab-grid-active.svg",
    open: "switchTab",
  },
  {
    key: "cart",
    label: "商品篮",
    url: "/pages/cart/cart",
    icon: "/images/tab-basket.svg",
    iconActive: "/images/tab-basket-active.svg",
    open: "switchTab",
  },
  {
    key: "profile",
    label: "我的",
    url: "/pages/profile/profile",
    icon: "/images/tab-user.svg",
    iconActive: "/images/tab-user-active.svg",
    open: "switchTab",
  },
];

const MERCHANT = [
  {
    key: "orders",
    label: "订单",
    url: "/packageMerchant/pages/orders/orders",
    icon: "/images/tab-list.svg",
    iconActive: "/images/tab-list-active.svg",
  },
  {
    key: "products",
    label: "商品",
    url: "/packageMerchant/pages/products/products",
    icon: "/images/tab-box.svg",
    iconActive: "/images/tab-box-active.svg",
  },
  {
    key: "settings",
    label: "设置",
    url: "/packageMerchant/pages/settings/settings",
    icon: "/images/tab-gear.svg",
    iconActive: "/images/tab-gear-active.svg",
  },
  {
    key: "profile",
    label: "我的",
    url: "/packageMerchant/pages/profile/profile",
    icon: "/images/tab-user.svg",
    iconActive: "/images/tab-user-active.svg",
  },
];

function customerTabs(entry) {
  if (entry !== "TAB") return CUSTOMER;
  return [CUSTOMER[0], CUSTOMER[1], MEMBER_CODE_TAB, CUSTOMER[2], CUSTOMER[3]];
}

function buildTabs(mode, entry) {
  const app = getApp();
  const cartCount = (app.globalData && app.globalData.cartCount) || 0;
  const urgent = (app.globalData && app.globalData.merchantUrgentCount) || 0;
  const inbox = (app.globalData && app.globalData.merchantInboxCount) || 0;
  const bubble = (app.globalData && app.globalData.merchantBubbleCount) || 0;
  const tabs = mode === "merchant" ? MERCHANT : customerTabs(entry);
  return tabs.map((t) => {
    let badge = 0;
    if (t.key === "cart") badge = cartCount;
    if (t.key === "orders") badge = urgent;
    // 站内信与冒泡的入口都在「我的」页，角标合并显示
    if (t.key === "profile" && mode === "merchant") badge = inbox + bubble;
    return { ...t, badge };
  });
}

const svgText = {};
const tintedFile = {};

/**
 * 商家端切 tab 仍走 reLaunch，页面会重建；连点两次会连续跳转。
 * 这里用模块级时间戳挡掉 400ms 内的重复点击。
 */
const NAV_LOCK_MS = 400;
let lastNavAt = 0;

/**
 * 底栏每次页面 show 都会 refreshTabs，而它要拿门店配置才知道会员码是不是
 * 中间的 tab。页面自己也在拉门店，这里再压一层节流，避免每切一次 tab 就多
 * 一个请求（真机上来回切几次就是请求风暴）。
 */
const STORE_REFRESH_MIN_INTERVAL = 30 * 1000;
let lastStoreRefreshAt = 0;

function navLock() {
  const now = Date.now();
  if (now - lastNavAt < NAV_LOCK_MS) return false;
  lastNavAt = now;
  return true;
}

function tokenColor(pageStyle, name, fallback) {
  const matched = new RegExp(name + ":([^;]+)").exec(pageStyle || "");
  return matched ? matched[1].trim() : fallback;
}

function readSvg(path) {
  if (svgText[path]) return svgText[path];
  const text = wx.getFileSystemManager().readFileSync(path, "utf8");
  svgText[path] = String(text);
  return svgText[path];
}

function toBase64(text) {
  const bytes = new Uint8Array(text.length);
  for (let i = 0; i < text.length; i++) bytes[i] = text.charCodeAt(i) & 0xff;
  return wx.arrayBufferToBase64(bytes.buffer);
}

/** 8 个图标 × 若干套配色；商家反复改主题时别让这份 base64 缓存无限长 */
const TINT_LIMIT = 48;

/** SVG 文件里内置的就是这两档颜色，命中时根本不需要染色 */
const STATIC_COLORS = { "#0F766E": true, "#5B7C78": true };

function tintedIcon(path, color) {
  const norm = String(color || "").toUpperCase();
  // 默认主题下直接给静态文件路径：省掉每页一次 10 张 base64 的跨线程传输，
  // 也省掉视图层对 10 个 data URI 的解码（文件可以被 WebView 缓存）
  if (STATIC_COLORS[norm]) return path;
  const key = path + "@" + norm;
  if (tintedFile[key]) return tintedFile[key];
  try {
    const tinted = readSvg(path).replace(/#(?:0F766E|5B7C78)/gi, norm);
    const uri = "data:image/svg+xml;base64," + toBase64(tinted);
    if (Object.keys(tintedFile).length >= TINT_LIMIT) {
      Object.keys(tintedFile).forEach((k) => delete tintedFile[k]);
    }
    tintedFile[key] = uri;
    return uri;
  } catch (e) {
    return path;
  }
}

function paintTabs(tabs, pageStyle) {
  const themePrimary = tokenColor(pageStyle, "--color-primary", "#0F766E");
  const themeMuted = tokenColor(pageStyle, "--color-muted", "#5B7C78");
  // 自定义 tabBar 挂在页面节点之外，拿不到 page 上的 CSS 变量，
  // 颜色只能由这里算好内联下去
  const themeLine = tokenColor(pageStyle, "--color-line", "#CCE5E1");
  return {
    themePrimary,
    themeMuted,
    themeLine,
    tabs: (tabs || []).map((t) => ({
      ...t,
      iconSrc: tintedIcon(t.icon, themeMuted),
      iconActiveSrc: tintedIcon(t.iconActive, themePrimary),
    })),
  };
}

Component({
  properties: {
    current: { type: String, value: "home" },
    mode: { type: String, value: "customer" },
  },
  data: {
    tabs: CUSTOMER,
    themePrimary: "#0F766E",
    themeMuted: "#5B7C78",
    /** 点击后先高亮目标项，不等新页面建好 */
    activeKey: "home",
  },
  observers: {
    mode() {
      this.refreshTabs();
      if (this.data.mode === "merchant") this.startMerchantBadgeWatch();
      else this.stopMerchantBadgeWatch();
    },
    current(v) {
      if (v && v !== this.data.activeKey) this.setData({ activeKey: v });
    },
  },
  pageLifetimes: {
    show() {
      this.refreshTabs();
      this.startMerchantBadgeWatch();
    },
    hide() {
      this.stopMerchantBadgeWatch();
    },
  },
  lifetimes: {
    attached() {
      this._alive = true;
      const current = this.data.current;
      if (current && current !== this.data.activeKey) this.setData({ activeKey: current });
      this.refreshTabs();
      this._offCart = onCartCountChange(() => this.refreshTabs());
      this._offTheme = themeApply.subscribe((pageStyle) => this.applyTheme(pageStyle));
      if (this.data.mode === "merchant") {
        this._offMerchant = merchantBadge.onChange(() => this.refreshTabs());
        this.startMerchantBadgeWatch();
      }
    },
    detached() {
      // 页面被 reLaunch 关掉后，残留回调再 setData 只会白白占桥接带宽
      this._alive = false;
      this.stopMerchantBadgeWatch();
      if (this._offCart) {
        this._offCart();
        this._offCart = null;
      }
      if (this._offTheme) {
        this._offTheme();
        this._offTheme = null;
      }
      if (this._offMerchant) {
        this._offMerchant();
        this._offMerchant = null;
      }
    },
  },
  methods: {
    /** 商家端：定时拉站内信 + 冒泡数量，让底栏角标在任何 tab 页都能自己更新 */
    startMerchantBadgeWatch() {
      if (!this._alive || this.data.mode !== "merchant" || this._badgeTimer) return;
      merchantBadge.pull();
      this._badgeTimer = setInterval(() => merchantBadge.pull(), MERCHANT_BADGE_INTERVAL);
    },
    stopMerchantBadgeWatch() {
      if (this._badgeTimer) {
        clearInterval(this._badgeTimer);
        this._badgeTimer = null;
      }
    },
    applyTheme(pageStyle) {
      if (!this.isOnTabPage()) return;
      this.paint(paintTabs(this.data.tabs, pageStyle || themeApply.painted()));
    },
    /** 图标是 data URI，内容没变就别再往视图层塞一遍 */
    paint(next) {
      if (!this._alive) return;
      const key =
        next.themePrimary +
        "|" +
        next.themeMuted +
        "|" +
        next.themeLine +
        "|" +
        (next.tabs || []).map((t) => t.key + ":" + t.badge).join(",");
      if (key === this._paintKey) return;
      this._paintKey = key;
      this.setData(next);
    },
    /** 栈顶是不是应该有底栏的页面；商家端底栏是页面内组件，一律算可见 */
    isOnTabPage() {
      if (this.data.mode === "merchant") return true;
      const pages = getCurrentPages();
      const route = (pages[pages.length - 1] || {}).route || "";
      // 拿不到路由时保守地照常刷新，避免判断不出来就把底栏冻在旧样子
      if (!route) return true;
      return !!KEY_BY_ROUTE[route];
    },
    refreshTabs() {
      if (!this._alive) return;
      // 微信会给每个页面（包括分包页）都创建一个 custom-tab-bar 实例。分包页上
      // 那个底栏根本看不见，却照样跟着主题/购物篮变化重算 10 张图标；进过的
      // 分包页越多，每次事件要干的活就越多，真机上就是越用越卡。
      if (!this.isOnTabPage()) return;
      const mode = this.data.mode;
      const cached = memberCodeEntry({ memberCodeEntry: getApp().globalData.memberCodeEntry });
      const style = themeApply.painted();
      this.paint(paintTabs(buildTabs(mode, cached), style));
      if (mode === "merchant") return;
      // 会员码位置已知时，30 秒内不再为底栏重拉门店
      if (
        getApp().globalData.memberCodeEntry &&
        Date.now() - lastStoreRefreshAt < STORE_REFRESH_MIN_INTERVAL
      ) {
        return;
      }
      lastStoreRefreshAt = Date.now();
      const seq = (this._tabSeq = (this._tabSeq || 0) + 1);
      api
        .store()
        .then((store) => {
          if (seq !== this._tabSeq) return;
          const entry = memberCodeEntry(store);
          if (entry === getApp().globalData.memberCodeEntry) return;
          getApp().globalData.memberCodeEntry = entry;
          this.paint(paintTabs(buildTabs(mode, entry), themeApply.painted()));
        })
        .catch(() => {});
    },
    onTap(e) {
      const url = e.currentTarget.dataset.url;
      const key = e.currentTarget.dataset.key;
      const open = e.currentTarget.dataset.open;
      if (!url) return;
      if (open === "push") {
        if (this._pushing) return;
        this._pushing = true;
        wx.navigateTo({
          url,
          complete: () => {
            this._pushing = false;
          },
        });
        return;
      }
      if (!navLock()) return;
      // 点的就是当前 tab，不用再跳一次
      if (key && key === this.data.activeKey) return;
      // 先亮起来，再切页
      this.setData({ activeKey: key || this.data.activeKey });
      if (open === "switchTab") {
        goTab(url);
        return;
      }
      // 商家端切 tab 用 redirectTo 替换当前页。reLaunch 会把整个小程序实例
      // 销毁重建，真机上切两次就开始卡顿、白屏。
      wx.redirectTo({
        url,
        fail: () => wx.reLaunch({ url }),
      });
    },
  },
});
