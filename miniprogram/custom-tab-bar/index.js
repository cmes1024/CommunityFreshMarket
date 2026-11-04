/**
 * 真正的 tabBar（app.json 里 custom: true）。
 * 好处：switchTab 复用页面实例，不再 reLaunch 反复销毁重建。
 * 具体外观仍复用 components/TabBar。
 *
 * 高亮由各 tab 页的 onShow 显式声明（utils/tabbar.syncTabBar）；这里只在
 * 页面还没来得及声明时（组件刚挂载）按路由兜底猜一次。
 */
const { keyOfRoute } = require("../utils/tabbar");

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
    /** 页面 onShow 传 key；生命周期兜底时沿用页面声明过的 key，不再重新猜。 */
    setActive(key) {
      if (key) this._pageKey = key;
      const next = key || this._pageKey || this.guessKey();
      if (!next) return;
      if (next !== this.data.activeKey) this.setData({ activeKey: next });
      // 点击时子组件会乐观高亮；父级值没变时 current 的 observer 不会触发，
      // 这里直接把子组件也拉回正确状态，避免快速来回切 tab 后残留旧高亮。
      const bar = this.selectComponent("#tabbar");
      if (bar && bar.data.activeKey !== next) bar.setData({ activeKey: next });
    },
    guessKey() {
      const pages = getCurrentPages();
      const page = pages[pages.length - 1];
      return keyOfRoute((page && page.route) || "");
    },
  },
});
