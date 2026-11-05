const theme = require("./theme");

const STORAGE_KEY = "cfm_theme";
/** 用 Set 而不是数组：同一个回调重复订阅不会叠加 */
const listeners = new Set();
let paintedStyle = "";

function readCache() {
  try {
    const raw = wx.getStorageSync(STORAGE_KEY);
    if (raw && raw.pageStyle && raw.background) return raw;
  } catch (e) {}
  return null;
}

function appData() {
  try {
    const app = getApp();
    if (app && app.globalData && app.globalData.themePageStyle) {
      return {
        pageStyle: app.globalData.themePageStyle,
        background: app.globalData.themeBackground || "#f0fdfa",
      };
    }
  } catch (e) {}
  return null;
}

function current() {
  return appData() || readCache() || fromPalette(theme.resolve(null));
}

function fromPalette(palette) {
  return { pageStyle: theme.pageStyle(palette), background: palette.background };
}

function emit(pageStyle) {
  if (pageStyle) paintedStyle = pageStyle;
  Array.from(listeners).forEach((fn) => {
    try {
      fn(paintedStyle);
    } catch (e) {
      // 组件销毁后回调可能抛错，但解绑本来就该走 detached；
      // 在这里删会把"还活着的底栏"永久摘掉，导致主题不再更新
    }
  });
}

/**
 * 订阅上限：万一某个组件销毁时没走到 detached，也不会无限累积拖慢 emit。
 * 正常情况下每条订阅都对应一个页面里的底栏实例（页面栈上限 10 层），
 * 64 条只在异常时才会碰到，避免把首页这类还活着的订阅误删。
 */
const MAX_LISTENERS = 64;

function subscribe(fn) {
  if (listeners.size >= MAX_LISTENERS) {
    const oldest = listeners.values().next();
    if (!oldest.done) listeners.delete(oldest.value);
  }
  listeners.add(fn);
  return function unsubscribe() {
    listeners.delete(fn);
  };
}

function paint(pageStyle) {
  emit(pageStyle);
}

function painted() {
  return paintedStyle || current().pageStyle;
}

function publish(pack) {
  // 主题没变就什么都不用做：避免每次拉门店都同步写缓存、遍历所有页面
  const current = appData();
  if (current && current.pageStyle === pack.pageStyle && current.background === pack.background) {
    return;
  }
  try {
    wx.setStorageSync(STORAGE_KEY, pack);
  } catch (e) {}
  try {
    const app = getApp();
    if (app && app.globalData) {
      app.globalData.themePageStyle = pack.pageStyle;
      app.globalData.themeBackground = pack.background;
    }
  } catch (e) {}
  if (typeof getCurrentPages === "function") {
    getCurrentPages().forEach((page) => {
      if (!page || typeof page.setData !== "function") return;
      const data = page.data || {};
      if (data.themePageStyle === pack.pageStyle && data.themeBackground === pack.background) return;
      page.setData({ themePageStyle: pack.pageStyle, themeBackground: pack.background });
    });
  }
  emit(pack.pageStyle);
}

function applyStore(store) {
  publish(fromPalette(theme.resolve(store)));
}

function hydrate() {
  publish(readCache() || fromPalette(theme.resolve(null)));
}

module.exports = { current, applyStore, hydrate, paint, painted, subscribe };
