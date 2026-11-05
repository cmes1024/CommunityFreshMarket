/**
 * 真机取证用的小计数板（临时）。
 * 逻辑层跑在手机上，模拟器读数不代表真机；这里把 setData 频率、请求频率、
 * 页面栈深度直接画在首页左上角，用手机截图就能判断到底谁在疯狂工作。
 */
/** 默认关闭：只在需要真机取证时改成 true，否则它自己就是每秒一次 setData */
const ENABLED = false;

const state = {
  setData: 0,
  maxKeys: 0,
  req: 0,
  since: Date.now(),
};

let snapshot = null;

function countSetData(patch) {
  if (!ENABLED) return;
  // 面板自己的刷新不算在内
  if (patch && Object.prototype.hasOwnProperty.call(patch, "hud")) return;
  state.setData += 1;
  let keys = 0;
  try {
    keys = Object.keys(patch || {}).length;
  } catch (e) {
    keys = 0;
  }
  if (keys > state.maxKeys) state.maxKeys = keys;
}

function countRequest() {
  if (!ENABLED) return;
  state.req += 1;
}

/** 每秒调用一次：返回上一秒的读数并清零 */
function sample() {
  const now = Date.now();
  const dt = Math.max(1, now - state.since) / 1000;
  snapshot = {
    setDataPerSec: Math.round(state.setData / dt),
    reqPerSec: Math.round(state.req / dt),
    maxKeys: state.maxKeys,
    stack: (getCurrentPages() || []).length,
  };
  state.setData = 0;
  state.maxKeys = 0;
  state.req = 0;
  state.since = now;
  return snapshot;
}

function install(instance) {
  if (!ENABLED || !instance || instance.__hud) return;
  instance.__hud = true;
  const orig = instance.setData;
  instance.setData = function (patch, cb) {
    countSetData(patch);
    return orig.call(this, patch, cb);
  };
}

module.exports = { install, countSetData, countRequest, sample, enabled: ENABLED };
