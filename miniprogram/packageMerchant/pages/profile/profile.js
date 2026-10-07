const api = require("../../../utils/api");
const copy = require("../../../utils/copy");
const { storeDisplayName } = require("../../../utils/copy");

Page({
  data: {
    user: {},
    avatarText: "店",
    rolePrefix: copy.merchant.staffPrefix,
    storeName: copy.storeName,
  },
  async onShow() {
    try {
      const [user, store] = await Promise.all([api.me(), api.store()]);
      const role = user.role === "OWNER" ? copy.merchant.ownerPrefix : copy.merchant.staffPrefix;
      const storeName = storeDisplayName(store);
      const nick = user.nickname || role;
      this.setData({
        user,
        rolePrefix: role,
        storeName,
        avatarText: nick.slice(0, 1),
      });
    } catch (e) {}
  },
  onInvite() {
    wx.showToast({ title: copy.merchant.inviteHint, icon: "none" });
  },
  backCustomer() {
    wx.reLaunch({ url: "/packageCustomer/pages/home/home" });
  },
});
