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
  backCustomer() {
    wx.reLaunch({ url: "/packageCustomer/pages/home/home" });
  },
  noop() {},
});
