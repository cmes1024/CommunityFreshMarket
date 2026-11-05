const api = require("../../utils/api");
const copy = require("../../utils/copy");
const { setUser, getUser } = require("../../utils/request");
const { uploadImage, absUrl } = require("../../utils/upload");
const { fenToYuan } = require("../../utils/money");
const { memberCodeEntry } = require("../../utils/memberCodeEntry");
const { syncTabBar } = require("../../utils/tabbar");
const { push } = require("../../utils/nav");

function greetName(name) {
  const hour = new Date().getHours();
  const template =
    hour < 12 ? copy.profile.morning : hour < 18 ? copy.profile.afternoon : copy.profile.evening;
  return copy.format(template, { name });
}

Page({
  data: {
    user: {},
    avatarText: copy.storeName.slice(0, 1),
    greetName: greetName(copy.profile.defaultNickname),
    avatarSrc: "",
    sheet: "",
    sheetOpen: false,
    /* page-container 常驻会在真机上盖住下一个页面（点功能入口白屏），只有
       用到时才挂载；sheetOpen 负责滑入滑出动画，sheetMounted 负责存在与否 */
    sheetMounted: false,
    nicknameInput: "",
    draftAvatarSrc: "",
    draftAvatarFile: "",
    draftInitial: "",
    profileSaving: false,
    contactPhone: "",
    showMemberCode: true,
    pointsValue: 0,
    balanceYuan: "0.00",
  },
  onLoad() {
    // 首帧用本地用户和缓存数据渲染，切 tab 不白屏
    const cached = getUser();
    if (cached) this.applyUser(cached);
    const store = api.peekStore();
    const points = api.peekPoints();
    const balance = api.peekBalance();
    this.setData({
      contactPhone: (store && store.contactPhone) || "",
      showMemberCode: store ? memberCodeEntry(store) !== "TAB" : true,
      pointsValue: points && points.balance != null ? points.balance : 0,
      balanceYuan: fenToYuan(balance && balance.balanceFen),
    });
  },
  async onShow() {
    syncTabBar(this, "profile");
    try {
      const [user, store, points, balance] = await Promise.all([
        api.me(),
        api.store().catch(() => ({})),
        api.pointsMe().catch(() => ({ balance: 0 })),
        api.balanceMe().catch(() => ({ balanceFen: 0 })),
      ]);
      this.applyUser(user);
      // 积分/余额/电话多半没变，只把真正变化的字段推给视图层
      const next = {
        contactPhone: (store && store.contactPhone) || "",
        showMemberCode: memberCodeEntry(store) !== "TAB",
        pointsValue: points && points.balance != null ? points.balance : 0,
        balanceYuan: fenToYuan(balance && balance.balanceFen),
      };
      const patch = {};
      Object.keys(next).forEach((k) => {
        if (this.data[k] !== next[k]) patch[k] = next[k];
      });
      if (Object.keys(patch).length) this.setData(patch);
    } catch (e) {}
  },
  applyUser(user) {
    if (!user) return;
    const prev = this._cachedUser || getUser();
    const changed =
      !prev ||
      prev.nickname !== user.nickname ||
      prev.avatarUrl !== user.avatarUrl ||
      !!prev.storeMember !== !!user.storeMember ||
      prev.manageStoreId !== user.manageStoreId;
    // setUser 走的是同步 storage 写入，切回「我的」时资料没变就别再阻塞一次
    if (changed) setUser(user);
    this._cachedUser = user;
    getApp().globalData.userInfo = user;
    getApp().globalData.isStoreMember = !!user.storeMember;
    getApp().globalData.manageStoreId = user.manageStoreId || null;
    const nick = (user.nickname || "").trim();
    const shown = nick || copy.profile.defaultNickname;
    const avatar = absUrl(user.avatarUrl);
    // 切回「我的」时用户资料通常原样返回（缓存命中，引用相同），别整块重推
    const same =
      this._paintedUser === user &&
      this.data.nicknameInput === nick &&
      this.data.avatarSrc === avatar &&
      this.data.avatarText === shown.slice(0, 1);
    this._paintedUser = user;
    if (same) return;
    this.setData({
      user,
      nicknameInput: nick,
      greetName: greetName(shown),
      avatarSrc: avatar,
      avatarText: shown.slice(0, 1),
    });
  },
  openProfileSheet() {
    const nick = ((this.data.user && this.data.user.nickname) || "").trim();
    const shown = nick || copy.profile.defaultNickname;
    this.setData({
      sheet: "profile",
      sheetMounted: true,
      nicknameInput: nick,
      draftAvatarSrc: this.data.avatarSrc,
      draftAvatarFile: "",
      draftInitial: shown.slice(0, 1),
    });
    // 先让容器挂载，再拉起来，滑入动画才不会被吞掉
    wx.nextTick(() => this.setData({ sheetOpen: true }));
  },
  closeSheet() {
    if (!this.data.sheetMounted) return;
    this.setData({ sheetOpen: false });
    if (this._closeTimer) clearTimeout(this._closeTimer);
    // 等滑出动画播完再卸载节点，避免动画被吞掉
    this._closeTimer = setTimeout(() => {
      this._closeTimer = null;
      if (!this.data.sheetOpen) this.setData({ sheet: "", sheetMounted: false });
    }, 260);
  },
  onSheetLeave() {
    if (this._closeTimer) {
      clearTimeout(this._closeTimer);
      this._closeTimer = null;
    }
    if (this.data.sheet || this.data.sheetOpen || this.data.sheetMounted) {
      this.setData({ sheet: "", sheetOpen: false, sheetMounted: false });
    }
  },
  onUnload() {
    if (this._closeTimer) clearTimeout(this._closeTimer);
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
      this.setData({ draftAvatarFile: "" });
      this.closeSheet();
      wx.showToast({ title: copy.profile.profileSaved, icon: "none" });
    } catch (err) {
      // 头像上传或提交失败时不能再静默，否则表现为「点了保存没反应」
      const message = (err && err.message) || copy.profile.profileSaveFailed;
      wx.showToast({ title: message, icon: "none" });
    } finally {
      this.setData({ profileSaving: false });
    }
  },
  goPoints() {
    push("/packageCustomer/pages/points/points");
  },
  goBalance() {
    push("/packageCustomer/pages/balance/balance");
  },
  goCoupons() {
    push("/packageCustomer/pages/coupons/coupons");
  },
  goMemberCode() {
    push("/packageCustomer/pages/member-code/member-code");
  },
  goOrders() {
    push("/packageCustomer/pages/orders/orders");
  },
  goAddr() {
    push("/packageCustomer/pages/address-list/address-list");
  },
  goMerchant() {
    // 和商家端切 tab 一致：redirectTo 只替换当前页，不重建整个小程序
    wx.redirectTo({
      url: "/packageMerchant/pages/orders/orders",
      fail: () => wx.reLaunch({ url: "/packageMerchant/pages/orders/orders" }),
    });
  },
  openContactSheet() {
    this.setData({ sheet: "contact", sheetMounted: true });
    wx.nextTick(() => this.setData({ sheetOpen: true }));
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
