const api = require("../../../utils/api");
const copy = require("../../../utils/copy");
const { storeDisplayName } = require("../../../utils/copy");

function toQrSrc(res) {
  const b64 = res && res.qrcodeBase64;
  if (!b64) return "";
  return `data:image/png;base64,${b64}`;
}

Page({
  data: {
    user: {},
    avatarText: "店",
    rolePrefix: copy.merchant.staffPrefix,
    storeName: copy.storeName,
    inviteShow: false,
    inviteCode: "",
    inviteUrl: "",
    inviteQrSrc: "",
    inboxCount: 0,
    bubbleCount: 0,
  },
  onShow() {
    this.loadProfile();
    this.startBubbleWatch();
  },
  onHide() {
    this.stopBubbleWatch();
  },
  onUnload() {
    this.stopBubbleWatch();
  },
  startBubbleWatch() {
    this.stopBubbleWatch();
    this.refreshBubbles();
    this._bubbleTimer = setInterval(() => this.refreshBubbles(), 5000);
  },
  stopBubbleWatch() {
    if (this._bubbleTimer) {
      clearInterval(this._bubbleTimer);
      this._bubbleTimer = null;
    }
  },
  async refreshBubbles() {
    try {
      const list = await api.merchantCheckinsActive();
      this.setData({ bubbleCount: (list || []).length });
    } catch (e) {}
  },
  async loadProfile() {
    try {
      const [user, store, requests] = await Promise.all([
        api.me(),
        api.store(),
        api.merchantLivePhotoRequests().catch(() => []),
      ]);
      const role = user.role === "OWNER" ? copy.merchant.ownerPrefix : copy.merchant.staffPrefix;
      const storeName = storeDisplayName(store);
      const nick = user.nickname || role;
      const inboxCount = (requests || []).length;
      this.setData({
        user,
        rolePrefix: role,
        storeName,
        avatarText: nick.slice(0, 1),
        inboxCount,
      });
    } catch (e) {}
  },
  applyInvite(res) {
    this.setData({
      inviteShow: true,
      inviteCode: (res && res.code) || "",
      inviteUrl: (res && res.inviteUrl) || "",
      inviteQrSrc: toQrSrc(res),
    });
  },
  async onInvite() {
    try {
      const res = await api.merchantInvite();
      this.applyInvite(res);
    } catch (e) {
      wx.showToast({ title: copy.merchant.inviteLoadFail, icon: "none" });
    }
  },
  closeInvite() {
    if (this.data.inviteShow) this.setData({ inviteShow: false });
  },
  async rotateInvite() {
    try {
      const res = await api.merchantInviteRotate();
      this.applyInvite(res);
      wx.showToast({ title: copy.merchant.inviteRotated, icon: "none" });
    } catch (e) {}
  },
  goInbox() {
    wx.navigateTo({ url: "/packageMerchant/pages/live-photos/live-photos" });
  },
  goCounter() {
    wx.navigateTo({ url: "/packageMerchant/pages/point-counter/point-counter" });
  },
  goBalance() {
    wx.navigateTo({ url: "/packageMerchant/pages/balance-topup/balance-topup" });
  },
  goCoupons() {
    wx.navigateTo({ url: "/packageMerchant/pages/coupons/coupons" });
  },
  goPointRedeems() {
    wx.navigateTo({ url: "/packageMerchant/pages/point-redeems/point-redeems" });
  },
  backCustomer() {
    wx.reLaunch({ url: "/packageCustomer/pages/home/home" });
  },
  noop() {},
});
