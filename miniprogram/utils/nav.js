/**
 * 切到 tabBar 页。switchTab 会复用页面实例（不销毁重建），
 * 极端情况下失败（如 tabBar 配置异常）退回 reLaunch，保证能跳过去。
 */
function goTab(url) {
  wx.switchTab({
    url,
    fail: () => wx.reLaunch({ url }),
  });
}

module.exports = { goTab };
