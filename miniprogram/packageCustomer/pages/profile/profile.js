const api = require("../../../utils/api");
const copy = require("../../../utils/copy");
const { setUser } = require("../../../utils/request");
const { uploadImage, absUrl } = require("../../../utils/upload");
const { memberCodeEntry } = require("../../../utils/memberCodeEntry");

Page({
  data: {
    user: {},
    avatarText: copy.storeName.slice(0, 1),
    avatarSrc: "",
    sheet: "",
    sheetOpen: false,
    nicknameInput: "",
    draftAvatarSrc: "",
    draftAvatarFile: "",
    draftInitial: "",
    profileSaving: false,
    contactPhone: "",
    showMemberCode: true,
  },
  async onShow() {
    try {
      const [user, store] = await Promise.all([api.me(), api.store().catch(() => ({}))]);
      this.applyUser(user);
      this.setData({
        contactPhone: (store && store.contactPhone) || "",
        showMemberCode: memberCodeEntry(store) !== "TAB",
      });
    } catch (e) {}
  },
  applyUser(user) {
    setUser(user);
    getApp().globalData.userInfo = user;
    getApp().globalData.isStoreMember = !!user.storeMember;
    getApp().globalData.manageStoreId = user.manageStoreId || null;
    const nick = (user.nickname || "").trim();
    const shown = nick || copy.profile.defaultNickname;
    this.setData({
      user,
      nicknameInput: nick,
      avatarSrc: absUrl(user.avatarUrl),
      avatarText: shown.slice(0, 1),
    });
  },
  openProfileSheet() {
    const nick = ((this.data.user && this.data.user.nickname) || "").trim();
    const shown = nick || copy.profile.defaultNickname;
    this.setData({
      sheet: "profile",
      sheetOpen: true,
      nicknameInput: nick,
      draftAvatarSrc: this.data.avatarSrc,
      draftAvatarFile: "",
      draftInitial: shown.slice(0, 1),
    });
  },
  closeSheet() {
    if (this.data.sheetOpen) this.setData({ sheetOpen: false });
  },
  onSheetLeave() {
    if (this.data.sheet || this.data.sheetOpen) this.setData({ sheet: "", sheetOpen: false });
  },
  onChooseAvatar(e) {
    const path = e.detail && e.detail.avatarUrl;
    if (!path) return;
    this.setData({ draftAvatarSrc: path, draftAvatarFile: path });
  },
  onNickInput(e) {
    this.setData({ nicknameInput: (e.detail && e.detail.value) || "" });
  },
  async saveProfile() {
    if (this.data.profileSaving) return;
    const nickname = (this.data.nicknameInput || "").trim();
    const current = ((this.data.user && this.data.user.nickname) || "").trim();
    const patch = {};
    if (nickname !== current) {
      if (!nickname) {
        wx.showToast({ title: copy.profile.nicknameRequired, icon: "none" });
        return;
      }
      patch.nickname = nickname;
    }
    if (!patch.nickname && !this.data.draftAvatarFile) {
      this.closeSheet();
      return;
    }
    this.setData({ profileSaving: true });
    try {
      if (this.data.draftAvatarFile) {
        patch.avatarUrl = await uploadImage(this.data.draftAvatarFile);
      }
      const user = await api.updateMe(patch);
      this.applyUser(user);
      this.setData({ sheetOpen: false, draftAvatarFile: "" });
      wx.showToast({ title: copy.profile.profileSaved, icon: "none" });
    } catch (err) {
    } finally {
      this.setData({ profileSaving: false });
    }
  },
  goPoints() {
    wx.navigateTo({ url: "/packageCustomer/pages/points/points" });
  },
  goBalance() {
    wx.navigateTo({ url: "/packageCustomer/pages/balance/balance" });
  },
  goCoupons() {
    wx.navigateTo({ url: "/packageCustomer/pages/coupons/coupons" });
  },
  goMemberCode() {
    wx.navigateTo({ url: "/packageCustomer/pages/member-code/member-code" });
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
    this.setData({ sheet: "contact", sheetOpen: true });
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
