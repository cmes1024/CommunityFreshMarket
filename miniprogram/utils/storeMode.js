/**
 * 顾客端形态由门店配置决定：
 * FULL     完整版：显示价格、商品篮、下单流程
 * SHOWCASE 橱窗版：仅展示商品，到店购买，不出现交易与会员入口
 *
 * 判断结果缓存在 globalData.showcase，页面每次拿到 store 后重新落一次。
 * 形态变化时顺带刷新当前页底栏，避免切完配置后底栏还是旧的那几项。
 */
const SHOWCASE = "SHOWCASE";

function isShowcase(store) {
  return String((store && store.customerMode) || "").toUpperCase() === SHOWCASE;
}

function refreshTabBar() {
  const pages = getCurrentPages ? getCurrentPages() : [];
  const page = pages[pages.length - 1];
  if (!page || typeof page.getTabBar !== "function") return;
  const bar = page.getTabBar();
  if (bar && typeof bar.refreshTabs === "function") bar.refreshTabs();
}

/** 用门店数据刷新全局形态，返回是否为橱窗版 */
function apply(store) {
  const showcase = isShowcase(store);
  const app = getApp();
  if (!app || !app.globalData) return showcase;
  const changed = !!app.globalData.showcase !== showcase;
  app.globalData.showcase = showcase;
  if (changed) refreshTabBar();
  return showcase;
}

function current() {
  const app = getApp();
  return !!(app && app.globalData && app.globalData.showcase);
}

module.exports = { isShowcase, apply, current };
