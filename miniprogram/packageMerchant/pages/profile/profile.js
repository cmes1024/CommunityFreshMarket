const api = require("../../../utils/api");
const copy = require("../../../utils/copy");
const merchantBadge = require("../../../utils/merchantBadge");
const { storeDisplayName } = require("../../../utils/copy");
const { goTab } = require("../../../utils/nav");

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
    /* page-container 常驻会在真机上盖住下一个页面（点功能入口白屏），只有
       用到时才挂载；inviteShow 负责滑入滑出动画，inviteMounted 负责存在与否 */
    inviteMounted: false,
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
    this.clearCloseTimer();
  },
  onUnload() {
    this.stopBubbleWatch();
    this.clearCloseTimer();
  },
  clearCloseTimer() {
    if (this._closeTimer) {
      clearTimeout(this._closeTimer);
      this._closeTimer = null;
    }
  },
  /**
   * 底栏在商家模式下已经每 15 秒拉一次冒泡 + 站内信数量，页面再起一个 5 秒
   * 定时器，等于把同一批接口拉两遍（真机上就是持续的卡顿和延迟）。这里改成
   * 只订阅角标数据源，页面不再自己轮询。
   */
  startBubbleWatch() {
    this.stopBubbleWatch();
    this._offBadge = merchantBadge.onChange((state) => this.applyBadge(state));
    this.applyBadge(merchantBadge.snapshot());
    merchantBadge.pull();
  },
  stopBubbleWatch() {
    if (this._offBadge) {
      this._offBadge();
      this._offBadge = null;
    }
  },
  applyBadge(state) {
    const bubble = Number((state && state.bubble) || 0);
    const inbox = Number((state && state.inbox) || 0);
    const patch = {};
    if (bubble !== this.data.bubbleCount) patch.bubbleCount = bubble;
    if (inbox !== this.data.inboxCount) patch.inboxCount = inbox;
    if (Object.keys(patch).length) this.setData(patch);
  },
  async loadProfile() {
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
      // 站内信数量也交给角标统一拉，不在页面里重复请求
      merchantBadge.pull();
    } catch (e) {}
  },
  applyInvite(res) {
    this.setData({
      inviteMounted: true,
      inviteCode: (res && res.code) || "",
      inviteUrl: (res && res.inviteUrl) || "",
      inviteQrSrc: toQrSrc(res),
    });
    // 先让容器挂载，再拉起来，滑入动画才不会被吞掉
    wx.nextTick(() => this.setData({ inviteShow: true }));
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
    if (!this.data.inviteMounted) return;
    this.setData({ inviteShow: false });
    this.clearCloseTimer();
    // 等滑出动画播完再卸载节点，避免动画被吞掉
    this._closeTimer = setTimeout(() => {
      this._closeTimer = null;
      if (!this.data.inviteShow) this.setData({ inviteMounted: false });
    }, 260);
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
    goTab("/pages/home/home");
  },
  noop() {},
});
