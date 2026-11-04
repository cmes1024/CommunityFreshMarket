/**
 * 真正的 tabBar（app.json 里 custom: true）。
 * 好处：switchTab 复用页面实例，不再 reLaunch 反复销毁重建，
 * 切 tab 不会白屏，也不会越用越卡。
 * 具体外观仍复用 components/TabBar。
 */
const KEY_BY_ROUTE = {
  "pages/home/home": "home",
  "pages/category/category": "category",
  "pages/cart/cart": "cart",
  "pages/profile/profile": "profile",
};

function currentKey() {
  const pages = getCurrentPages();
  const page = pages[pages.length - 1];
  const route = (page && page.route) || "";
  return KEY_BY_ROUTE[route] || "home";
}

Component({
  data: {
    activeKey: "home",
  },
  lifetimes: {
    attached() {
      this.setActive();
    },
  },
  pageLifetimes: {
    show() {
      this.setActive();
    },
  },
  methods: {
    setActive() {
      const key = currentKey();
      if (key !== this.data.activeKey) this.setData({ activeKey: key });
    },
  },
});
