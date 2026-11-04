/**
 * 轻量内存缓存：只存「读多写少」的接口结果。
 * - read：只返回未过期的值（接口层用，过期就重新拉）
 * - peek：过期也返回旧值（页面首帧用，先出内容再校准，避免切页白屏）
 * ttl 为 0 表示不过期，由调用方显式 drop（例如购物篮快照）。
 * 小程序 reLaunch / 切页不会重启 JS 运行时，模块级变量在会话内一直有效。
 */
const buckets = new Map();

/** 商品详情、各分类列表会随浏览不断新增，给个上限防止越用越占内存 */
const MAX_ENTRIES = 80;

function entryOf(key) {
  return buckets.get(key) || null;
}

function fresh(entry) {
  return !!entry && (!entry.expireAt || entry.expireAt > Date.now());
}

function read(key) {
  const hit = entryOf(key);
  return fresh(hit) ? hit.value : null;
}

/** 允许拿过期值：用于首帧渲染 */
function peek(key) {
  const hit = entryOf(key);
  return hit ? hit.value : null;
}

function write(key, value, ttlMs) {
  if (buckets.has(key)) buckets.delete(key);
  else if (buckets.size >= MAX_ENTRIES) {
    // 淘汰最早写入的一条（Map 按写入顺序迭代）
    const oldest = buckets.keys().next();
    if (!oldest.done) buckets.delete(oldest.value);
  }
  buckets.set(key, {
    value,
    expireAt: ttlMs ? Date.now() + ttlMs : 0,
  });
  return value;
}

function drop(key) {
  buckets.delete(key);
}

function dropPrefix(prefix) {
  Array.from(buckets.keys()).forEach((key) => {
    if (key.indexOf(prefix) === 0) buckets.delete(key);
  });
}

function clear() {
  buckets.clear();
}

module.exports = { read, peek, write, drop, dropPrefix, clear };
