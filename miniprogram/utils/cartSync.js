const api = require("./api");

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

function applyCartCount(cart) {
  const count = (cart && cart.itemCount) || 0;
  try {
    getApp().globalData.cartCount = count;
  } catch (e) {}
  notifyCartCount(count);
  return cart || null;
}

/** @param {object} [cartMaybe] 已有篮数据时可传入，避免重复请求 */
async function syncCartCount(cartMaybe) {
  try {
    const cart = cartMaybe || (await api.cart());
    return applyCartCount(cart);
  } catch (e) {
    return null;
  }
}

function qtyMap(cart) {
  const map = {};
  (cart?.items || []).forEach((line) => {
    map[line.productId] = Number(line.qty);
  });
  return map;
}

module.exports = { syncCartCount, applyCartCount, onCartCountChange, qtyMap };
