/**
 * 真正的 tabBar（app.json 里 custom: true）。
 * 好处：switchTab 复用页面实例，不再 reLaunch 反复销毁重建。
 * 具体外观仍复用 components/TabBar。
 *
 * 高亮由各 tab 页的 onShow 显式声明（utils/tabbar.syncTabBar）；这里只在
 * 页面还没来得及声明时（组件刚挂载）按路由兜底猜一次。
 *
 * 渲染时机：微信给每个页面都建一份 custom-tab-bar 实例，分包页那份看不见，
 * 却照样会渲染 5 个 tab 和 10 张 base64 图标 —— 每进一次商品详情、记积分
 * 都要白付一次。所以底栏内容默认不渲染，靠两个信号挂出来：
 *   1. tab 页 onShow 里的 syncTabBar 主动 showBar()（主路径）；
 *   2. 挂载后 200ms 自查一次，栈顶就是本页才挂（兜底，页面栈此时已稳定）。
 * 一旦挂出就不再隐藏，避免从分包页返回时底栏消失。
 *
 * 注意：不要用 wx:if 之外的手段（例如给容器加 hidden）来控制，那只是看不见，
 * 节点和图标照样要渲染。
 */
const { keyOfRoute } = require("../utils/tabbar");

Component({
  data: {
    activeKey: "home",
    showBar: false,
  },
  lifetimes: {
    attached() {
      // attached 时页面栈还停在上一个页面（实测是 launch），不能立刻判断
      this._retryTimer = setTimeout(() => {
        this._retryTimer = null;
        this.syncVisible();
      }, 200);
    },
    detached() {
      if (this._retryTimer) {
        clearTimeout(this._retryTimer);
        this._retryTimer = null;
      }
    },
  },
  methods: {
    /** tab 页 onShow 调用：直接把底栏挂出来 */
    showBar() {
      if (!this.data.showBar) this.setData({ showBar: true });
    },
    /** 兜底：栈顶是本页（tab 页）才挂；分包页那份实例永远走不到这里 */
    syncVisible() {
      if (this.data.showBar) return;
      const pages = getCurrentPages();
      const route = (pages[pages.length - 1] || {}).route || "";
      if (!route || keyOfRoute(route)) this.setData({ showBar: true });
    },
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
