const api = require("../../utils/api");
const { onCartCountChange } = require("../../utils/cartSync");
const { memberCodeEntry } = require("../../utils/memberCodeEntry");
const merchantBadge = require("../../utils/merchantBadge");
const themeApply = require("../../utils/themeApply");
const { goTab } = require("../../utils/nav");

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

function tintedIcon(path, color) {
  const key = path + "@" + color;
  if (tintedFile[key]) return tintedFile[key];
  try {
    const tinted = readSvg(path).replace(/#(?:0F766E|5B7C78)/gi, color);
    const uri = "data:image/svg+xml;base64," + toBase64(tinted);
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
    refreshTabs() {
      if (!this._alive) return;
      const mode = this.data.mode;
      const cached = memberCodeEntry({ memberCodeEntry: getApp().globalData.memberCodeEntry });
      const style = themeApply.painted();
      this.paint(paintTabs(buildTabs(mode, cached), style));
      if (mode === "merchant") return;
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
      // 先亮起来，再切页
      this.setData({ activeKey: key || this.data.activeKey });
      if (open === "switchTab") {
        goTab(url);
        return;
      }
      wx.reLaunch({ url });
    },
  },
});
