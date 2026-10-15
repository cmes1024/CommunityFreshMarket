const api = require("../../utils/api");
const { onCartCountChange } = require("../../utils/cartSync");
const { memberCodeEntry } = require("../../utils/memberCodeEntry");
const themeApply = require("../../utils/themeApply");

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
    url: "/packageCustomer/pages/home/home",
    icon: "/images/tab-home.svg",
    iconActive: "/images/tab-home-active.svg",
  },
  {
    key: "category",
    label: "分类",
    url: "/packageCustomer/pages/category/category",
    icon: "/images/tab-grid.svg",
    iconActive: "/images/tab-grid-active.svg",
  },
  {
    key: "cart",
    label: "商品篮",
    url: "/packageCustomer/pages/cart/cart",
    icon: "/images/tab-basket.svg",
    iconActive: "/images/tab-basket-active.svg",
  },
  {
    key: "profile",
    label: "我的",
    url: "/packageCustomer/pages/profile/profile",
    icon: "/images/tab-user.svg",
    iconActive: "/images/tab-user-active.svg",
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
  const tabs = mode === "merchant" ? MERCHANT : customerTabs(entry);
  return tabs.map((t) => {
    let badge = 0;
    if (t.key === "cart") badge = cartCount;
    if (t.key === "orders") badge = urgent;
    return { ...t, badge };
  });
}

const svgText = {};
const tintedFile = {};

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
  return {
    themePrimary,
    themeMuted,
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
  data: { tabs: CUSTOMER, themePrimary: "#0F766E", themeMuted: "#5B7C78" },
  observers: {
    mode() {
      this.refreshTabs();
    },
  },
  pageLifetimes: {
    show() {
      this.refreshTabs();
    },
  },
  lifetimes: {
    attached() {
      this.refreshTabs();
      this._offCart = onCartCountChange(() => this.refreshTabs());
      this._offTheme = themeApply.subscribe((pageStyle) => this.applyTheme(pageStyle));
    },
    detached() {
      if (this._offCart) {
        this._offCart();
        this._offCart = null;
      }
      if (this._offTheme) {
        this._offTheme();
        this._offTheme = null;
      }
    },
  },
  methods: {
    applyTheme(pageStyle) {
      this.setData(paintTabs(this.data.tabs, pageStyle || themeApply.painted()));
    },
    refreshTabs() {
      const mode = this.data.mode;
      const cached = memberCodeEntry({ memberCodeEntry: getApp().globalData.memberCodeEntry });
      const style = themeApply.painted();
      this.setData(paintTabs(buildTabs(mode, cached), style));
      if (mode === "merchant") return;
      const seq = (this._tabSeq = (this._tabSeq || 0) + 1);
      api
        .store()
        .then((store) => {
          if (seq !== this._tabSeq) return;
          const entry = memberCodeEntry(store);
          getApp().globalData.memberCodeEntry = entry;
          this.setData(paintTabs(buildTabs(mode, entry), themeApply.painted()));
        })
        .catch(() => {});
    },
    onTap(e) {
      const url = e.currentTarget.dataset.url;
      if (!url) return;
      if (e.currentTarget.dataset.open === "push") {
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
      wx.reLaunch({ url });
    },
  },
});
