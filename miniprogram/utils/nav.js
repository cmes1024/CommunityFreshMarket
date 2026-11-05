/**
 * 切到 tabBar 页。
 *
 * 从 reLaunch 改成 switchTab 的初衷就是别销毁重建页面。但 switchTab 会在
 * 「点的就是当前页」「上一次跳转还没结束」「快速连点」时走 fail，而 fail 里
 * 再 reLaunch 等于把刚省下的重建又做了一遍：真机上表现为白屏几秒、页面栈
 * 重新初始化、页面已经画出来却滚不动也点不动，过几秒才恢复。
 * 所以这里失败就失败，绝不重建整个小程序——让用户再点一次代价小得多。
 */
function goTab(url) {
  wx.switchTab({
    url,
    fail: (err) => {
      const msg = (err && err.errMsg) || "";
      if (!msg || msg.indexOf("current page") >= 0) return;
      console.warn("switchTab failed, stay on current page:", msg, url);
    },
  });
}

/**
 * 打开二级页。入口是整块卡片时手一抖会连点两次，navigateTo 不会自动去重，
 * 于是页面栈里叠出同样的页面（每个都带 Header、协议、底栏组件），
 * 真机上来回几次就明显变卡。这里按 url 做短间隔去重。
 */
const PUSH_LOCK_MS = 600;
let lastPushUrl = "";
let lastPushAt = 0;

/** 页面栈到这个深度就改用 redirectTo，别再往上叠 */
const PAGE_STACK_LIMIT = 8;

function push(url) {
  if (!url) return;
  const now = Date.now();
  if (url === lastPushUrl && now - lastPushAt < PUSH_LOCK_MS) return;
  lastPushAt = now;
  lastPushUrl = url;
  const options = {
    url,
    fail: () => {
      lastPushUrl = "";
    },
  };
  // 每多压一层都带着 Header / 底栏 / 图片一起留在内存里，真机上是叠加的；
  // 栈已经很深时用 redirectTo 换掉当前页，避免"点得越多越卡"
  const stack = (getCurrentPages() || []).length;
  if (stack >= PAGE_STACK_LIMIT) wx.redirectTo(options);
  else wx.navigateTo(options);
}

module.exports = { goTab, push };
