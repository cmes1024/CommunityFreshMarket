const copy = require("../../utils/copy");
const { getUser } = require("../../utils/request");
const { goTab } = require("../../utils/nav");

/** 冷启动最多为登录等这么久，等不到就先放人进首页 */
const LOGIN_WAIT_MS = 600;

Page({
  data: {
    enteringText: copy.format(copy.launch.entering),
  },
  onLoad() {
    const app = getApp();
    this.left = false;
    this.go = () => {
      if (this.left) return;
      this.left = true;
      if (this._waitTimer) {
        clearTimeout(this._waitTimer);
        this._waitTimer = null;
      }
      const member = !!(app.globalData && app.globalData.isStoreMember);
      if (member) {
        wx.reLaunch({ url: "/packageMerchant/pages/orders/orders" });
        return;
      }
      // 顾客首页是 tabBar 页，用 switchTab，之后切 tab 都复用页面实例
      goTab("/pages/home/home");
    };

    // 有本地用户就直接进，登录态在后台续上，不把冷启动卡在网络上
    if (getUser()) {
      this.go();
      return;
    }
    // 清缓存后的首次启动：以前这里死等登录请求，网络一慢就一直停在「进入中」。
    // 现在最多等 600ms，等不到先进首页；登录晚到且确认是店家时由 app.js 纠正落点。
    this._waitTimer = setTimeout(() => this.go(), LOGIN_WAIT_MS);
    if (app.loginPromise) app.loginPromise.finally(() => this.go());
    else this.go();
  },
  onUnload() {
    if (this._waitTimer) {
      clearTimeout(this._waitTimer);
      this._waitTimer = null;
    }
    this.left = true;
  },
});
