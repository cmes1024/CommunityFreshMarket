/**
 * 自定义 tabBar 的激活态由页面自己声明。
 *
 * 之前 custom-tab-bar 靠 getCurrentPages() 猜当前页：switchTab 时组件
 * attached / pageLifetimes.show 可能早于页面栈更新，栈顶还是上一个页面，
 * 于是人已经在「分类」，底栏却亮着「首页」。
 */
const KEY_BY_ROUTE = {
  "pages/home/home": "home",
  "pages/category/category": "category",
  "pages/cart/cart": "cart",
  "pages/profile/profile": "profile",
};

function keyOfRoute(route) {
  return KEY_BY_ROUTE[route] || "";
}

/** 在每个 tab 页的 onShow 里调用一次。 */
function syncTabBar(page, key) {
  if (!key || !page || typeof page.getTabBar !== "function") return;
  const bar = page.getTabBar();
  if (!bar) return;
  // 分包页也会有一份看不见的 custom-tab-bar 实例，底栏内容默认不渲染；
  // tab 页自己 show 时把它挂出来，分包页永远不调用这里
  if (typeof bar.showBar === "function") bar.showBar();
  if (typeof bar.setActive === "function") bar.setActive(key);
}

module.exports = { KEY_BY_ROUTE, keyOfRoute, syncTabBar };
