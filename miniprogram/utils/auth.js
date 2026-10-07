const { post, setToken, setUser, getUser } = require("./request");

let inflight = null;

function login(force = false) {
  if (!force && inflight) return inflight;
  inflight = (async () => {
    const { code } = await wx.login();
    const data = await post("/api/v1/auth/wechat", { code }, { skipAuth: true });
    setToken(data.token);
    setUser(data.user);
    const app = getApp();
    if (app && app.globalData) {
      app.globalData.userInfo = data.user;
      app.globalData.isStoreMember = !!data.user.storeMember;
      app.globalData.manageStoreId = data.user.manageStoreId || null;
      app.globalData.browseStoreId = data.user.browseStoreId;
    }
    notifyReady();
    return data.user;
  })().finally(() => {
    inflight = null;
  });
  return inflight;
}

function ensureLogin() {
  const cached = getUser();
  if (cached && getApp()?.globalData) {
    getApp().globalData.userInfo = cached;
    getApp().globalData.isStoreMember = !!cached.storeMember;
    getApp().globalData.manageStoreId = cached.manageStoreId || null;
    getApp().globalData.browseStoreId = cached.browseStoreId;
  }
  return login(false);
}

function notifyReady() {
  const app = getApp();
  if (!app) return;
  (app._authReadyCallbacks || []).forEach((fn) => fn());
  app._authReadyCallbacks = [];
}

function onAuthReady(callback) {
  const app = getApp();
  if (app?.globalData?.userInfo) {
    callback();
  } else {
    if (!app._authReadyCallbacks) app._authReadyCallbacks = [];
    app._authReadyCallbacks.push(callback);
  }
}

module.exports = { login, ensureLogin, onAuthReady };
