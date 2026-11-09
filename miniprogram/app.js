require("./extend/page/index");
require("./extend/component/index");
const { autoUpdate } = require("./utils/update.js");
const { RequestManager } = require("./utils/request");
const auth = require("./utils/auth");
const config = require("./utils/config");
const copy = require("./utils/copy");
const api = require("./utils/api");
const themeApply = require("./utils/themeApply");
const storeMode = require("./utils/storeMode");

App({
  onLaunch() {
    themeApply.hydrate();
    this.globalData.config = config;
    this.globalData.copy = copy;
    this.globalData.request = new RequestManager();
    this.globalData.request.preTreatment({ startRequest: true }, () => {});
    this.loginPromise = auth.ensureLogin()
      .then((user) => {
        api.store()
          .then((store) => storeMode.apply(store))
          .catch((err) => console.error("store theme", err));
        // launch 页最多等登录 600ms 就先进首页了：如果登录晚到、身份是店家，
        // 且用户还停在首页第一层，这里把它送去商家工作台
        if (user && this.globalData.isStoreMember) {
          const pages = getCurrentPages() || [];
          const top = pages[pages.length - 1];
          if (pages.length === 1 && top && top.route === "pages/home/home") {
            wx.reLaunch({ url: "/packageMerchant/pages/orders/orders" });
          }
        }
        return user;
      })
      .catch((err) => {
        console.error("login failed", err);
      });
  },

  onShow() {
    autoUpdate();
  },

  onError() {},

  globalData: {
    config: null,
    copy: null,
    userInfo: null,
    isStoreMember: false,
    manageStoreId: null,
    browseStoreId: 1,
    request: null,
    cartCount: 0,
    /** 顾客端形态：false 完整版 / true 橱窗版，见 utils/storeMode */
    showcase: false,
    merchantUrgentCount: 0,
    merchantInboxCount: 0,
    merchantBubbleCount: 0,
    /** 首页点分类 → switchTab 不能带参数，用它把选中的分类带过去 */
    pendingCategoryId: 0,
  },
});
