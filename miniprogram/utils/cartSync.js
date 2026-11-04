const api = require("./api");
const cache = require("./cache");

const SNAPSHOT_KEY = "cart:snapshot";

const listeners = new Set();

function notifyCartCount(count) {
  listeners.forEach((fn) => {
    try {
      fn(count);
    } catch (e) {}
  });
}

function onCartCountChange(fn) {
  if (typeof fn !== "function") return () => {};
  listeners.add(fn);
  return () => listeners.delete(fn);
}

/** 上一次拿到的购物篮，页面可同步取用先渲染，再发请求校准 */
function lastCart() {
  return cache.read(SNAPSHOT_KEY);
}

function applyCartCount(cart) {
  const count = (cart && cart.itemCount) || 0;
  try {
    getApp().globalData.cartCount = count;
  } catch (e) {}
  if (cart) cache.write(SNAPSHOT_KEY, cart, 0);
  notifyCartCount(count);
  return cart || null;
}

/** 下单成功后购物篮已清空，不用再请求一次 */
function clearCart() {
  const empty = { items: [], totalFen: 0, availableCount: 0, itemCount: 0 };
  cache.write(SNAPSHOT_KEY, empty, 0);
  notifyCartCount(0);
  try {
    getApp().globalData.cartCount = 0;
  } catch (e) {}
  return empty;
}

/** @param {object} [cartMaybe] 已有篮数据时可传入，避免重复请求 */
async function syncCartCount(cartMaybe) {
  try {
    const cart = cartMaybe || (await api.cart());
    return applyCartCount(cart);
  } catch (e) {
    return lastCart();
  }
}

/**
 * 改数量：同一商品串行发送，只把最后一次的值发给服务端，
 * 连点加号不会堆一串请求，也不会被旧响应覆盖。
 */
const desired = new Map();
const running = new Map();

function setQty(productId, qty) {
  const id = Number(productId);
  if (!id) return Promise.resolve(lastCart());
  const key = String(id);
  desired.set(key, qty);
  const pending = running.get(key);
  if (pending) return pending;

  const task = (async () => {
    let cart = null;
    while (desired.has(key)) {
      const target = desired.get(key);
      desired.delete(key);
      cart = await api.cartSetQty(id, target);
    }
    return applyCartCount(cart);
  })();

  running.set(key, task);
  return task.finally(() => {
    running.delete(key);
    desired.delete(key);
  });
}

function qtyMap(cart) {
  const map = {};
  (cart?.items || []).forEach((line) => {
    map[line.productId] = Number(line.qty);
  });
  return map;
}

module.exports = {
  syncCartCount,
  applyCartCount,
  clearCart,
  lastCart,
  setQty,
  onCartCountChange,
  qtyMap,
};
