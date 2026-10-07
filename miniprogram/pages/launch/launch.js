const copy = require("../../utils/copy");

Page({
  data: {
    enteringText: copy.format(copy.launch.entering),
  },
  onLoad() {
    const app = getApp();
    const go = () => {
      wx.reLaunch({ url: "/packageCustomer/pages/home/home" });
    };
    if (app.loginPromise) {
      app.loginPromise.finally(go);
    } else {
      go();
    }
  },
});
