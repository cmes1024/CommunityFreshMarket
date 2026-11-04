const copy = require("../../utils/copy");
const { getUser } = require("../../utils/request");
const { goTab } = require("../../utils/nav");

Page({
  data: {
    enteringText: copy.format(copy.launch.entering),
  },
  onLoad() {
    const app = getApp();
    const go = () => {
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
      go();
      return;
    }
    if (app.loginPromise) {
      app.loginPromise.finally(go);
    } else {
      go();
    }
  },
});
