const api = require("../../../utils/api");
const copy = require("../../../utils/copy");
const { setUser } = require("../../../utils/request");

function parseInviteCode(query) {
  if (!query) return "";
  if (query.code) return String(query.code).trim().toUpperCase();
  // 扫普通链接时偶发把整段 URL 放在 q
  if (query.q) {
    try {
      const raw = decodeURIComponent(String(query.q));
      const m = /[?&]code=([^&]+)/i.exec(raw);
      if (m && m[1]) return decodeURIComponent(m[1]).trim().toUpperCase();
    } catch (e) {}
  }
  if (query.scene) {
    try {
      return decodeURIComponent(String(query.scene)).trim().toUpperCase();
    } catch (e) {
      return String(query.scene).trim().toUpperCase();
    }
  }
  return "";
}

Page({
  data: {
    inviteCode: "",
    joining: false,
  },
  onLoad(query) {
    // 无常规入口；禁分享，避免扩散该页
    try {
      wx.hideShareMenu({ menus: ["shareAppMessage", "shareTimeline"] });
    } catch (e) {}
    const code = parseInviteCode(query);
    if (code) this.setData({ inviteCode: code });
  },
  onCode(e) {
    this.setData({ inviteCode: (e.detail.value || "").toUpperCase() });
  },
  async submit() {
    const code = (this.data.inviteCode || "").trim();
    if (!code) {
      wx.showToast({ title: copy.joinStore.needCode, icon: "none" });
      return;
    }
    if (this.data.joining) return;
    this.setData({ joining: true });
    try {
      const app = getApp();
      if (app.loginPromise) await app.loginPromise;
      const user = await api.acceptInvite(code);
      setUser(user);
      app.globalData.userInfo = user;
      app.globalData.isStoreMember = !!user.storeMember;
      app.globalData.manageStoreId = user.manageStoreId || null;
      wx.showToast({ title: copy.joinStore.ok, icon: "none" });
      setTimeout(() => {
        wx.reLaunch({ url: "/packageMerchant/pages/orders/orders" });
      }, 400);
    } catch (e) {
      this.setData({ joining: false });
    }
  },
});
