const api = require("../../../utils/api");
const copy = require("../../../utils/copy");
const { setUser } = require("../../../utils/request");

Page({
  data: {
    user: {},
    avatarText: copy.storeName.slice(0, 1),
    contactSheetShow: false,
    contactPhone: "",
  },
  async onShow() {
    try {
      const [user, store] = await Promise.all([api.me(), api.store().catch(() => ({}))]);
      setUser(user);
      getApp().globalData.userInfo = user;
      getApp().globalData.isStoreMember = !!user.storeMember;
      getApp().globalData.manageStoreId = user.manageStoreId || null;
      const nick = user.nickname || copy.profile.defaultNickname;
      this.setData({
        user,
        avatarText: nick.slice(0, 1),
        contactPhone: (store && store.contactPhone) || "",
      });
    } catch (e) {}
  },
  goPoints() {
    wx.navigateTo({ url: "/packageCustomer/pages/points/points" });
  },
  goCoupons() {
    wx.navigateTo({ url: "/packageCustomer/pages/coupons/coupons" });
  },
  async doCheckin() {
    let body = {};
    try {
      const loc = await new Promise((resolve, reject) => {
        wx.getLocation({ type: "gcj02", success: resolve, fail: reject });
      });
      body = { lat: loc.latitude, lng: loc.longitude };
    } catch (e) {}
    try {
      await api.checkin(body);
      wx.showToast({ title: copy.profile.checkinDone, icon: "none" });
    } catch (e) {
      wx.showToast({ title: copy.profile.checkinFail, icon: "none" });
    }
  },
  goOrders() {
    wx.navigateTo({ url: "/packageCustomer/pages/orders/orders" });
  },
  goAddr() {
    wx.navigateTo({ url: "/packageCustomer/pages/address-list/address-list" });
  },
  goMerchant() {
    wx.reLaunch({ url: "/packageMerchant/pages/orders/orders" });
  },
  openContactSheet() {
    this.setData({ contactSheetShow: true });
  },
  closeContactSheet() {
    if (this.data.contactSheetShow) this.setData({ contactSheetShow: false });
  },
  callStore() {
    const phone = (this.data.contactPhone || "").trim();
    if (!phone) {
      wx.showToast({ title: copy.profile.storePhoneEmpty, icon: "none" });
      return;
    }
    wx.makePhoneCall({ phoneNumber: phone });
  },
  noop() {},
});
