const theme = require("./theme");

const STORAGE_KEY = "cfm_theme";
const listeners = [];
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
  listeners.slice().forEach((fn) => {
    try {
      fn(paintedStyle);
    } catch (e) {}
  });
}

function subscribe(fn) {
  listeners.push(fn);
  return function unsubscribe() {
    const i = listeners.indexOf(fn);
    if (i >= 0) listeners.splice(i, 1);
  };
}

function paint(pageStyle) {
  emit(pageStyle);
}

function painted() {
  return paintedStyle || current().pageStyle;
}

function publish(pack) {
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
