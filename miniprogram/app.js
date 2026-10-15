require("./extend/page/index");
require("./extend/component/index");
const { autoUpdate } = require("./utils/update.js");
const { RequestManager } = require("./utils/request");
const auth = require("./utils/auth");
const config = require("./utils/config");
const copy = require("./utils/copy");
const api = require("./utils/api");
const themeApply = require("./utils/themeApply");

App({
  onLaunch() {
    themeApply.hydrate();
    this.globalData.config = config;
    this.globalData.copy = copy;
    this.globalData.request = new RequestManager();
    this.globalData.request.preTreatment({ startRequest: true }, () => {});
    this.loginPromise = auth.ensureLogin()
      .then((user) => {
        api.store().catch((err) => console.error("store theme", err));
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
    merchantUrgentCount: 0,
  },
});
