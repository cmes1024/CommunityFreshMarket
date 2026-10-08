const copy = require("../../utils/copy");

Page({
  data: {
    enteringText: copy.format(copy.launch.entering),
  },
  onLoad() {
    const app = getApp();
    const go = () => {
      const member = !!(app.globalData && app.globalData.isStoreMember);
      wx.reLaunch({
        url: member
          ? "/packageMerchant/pages/orders/orders"
          : "/packageCustomer/pages/home/home",
      });
    };
    if (app.loginPromise) {
      app.loginPromise.finally(go);
    } else {
      go();
    }
  },
});
