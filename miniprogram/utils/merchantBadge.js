/**
 * 商家端底栏角标的统一数据源：
 * - urgent：待处理订单
 * - inbox：站内信（待处理实景图申请）
 * - bubble：正在冒泡的顾客
 * 存在 globalData 里，TabBar 订阅变化后自己重绘，各页面只负责写入自己那份。
 */
const api = require("./api");

const listeners = new Set();

function globalData() {
  try {
    return getApp().globalData || {};
  } catch (e) {
    return {};
  }
}

function snapshot() {
  const g = globalData();
  return {
    urgent: Number(g.merchantUrgentCount) || 0,
    inbox: Number(g.merchantInboxCount) || 0,
    bubble: Number(g.merchantBubbleCount) || 0,
  };
}

function notify() {
  const state = snapshot();
  listeners.forEach((fn) => {
    try {
      fn(state);
    } catch (e) {}
  });
}

function set(part) {
  const g = globalData();
  let changed = false;
  const apply = (key, value) => {
    if (value == null) return;
    const n = Math.max(0, Number(value) || 0);
    if (g[key] !== n) {
      g[key] = n;
      changed = true;
    }
  };
  apply("merchantUrgentCount", part.urgent);
  apply("merchantInboxCount", part.inbox);
  apply("merchantBubbleCount", part.bubble);
  if (changed) notify();
  return snapshot();
}

/** 拉一次站内信 + 冒泡数量（订单数由订单台写入，接口太重不轮询） */
async function pull() {
  const [bubbles, requests] = await Promise.all([
    api.merchantCheckinsActive().catch(() => null),
    api.merchantLivePhotoRequests().catch(() => null),
  ]);
  const part = {};
  if (Array.isArray(bubbles)) part.bubble = bubbles.length;
  if (Array.isArray(requests)) part.inbox = requests.length;
  return set(part);
}

function onChange(fn) {
  if (typeof fn !== "function") return () => {};
  listeners.add(fn);
  return () => listeners.delete(fn);
}

module.exports = { snapshot, set, pull, onChange };
