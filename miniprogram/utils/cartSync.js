const api = require("./api");

async function syncCartCount() {
  try {
    const cart = await api.cart();
    getApp().globalData.cartCount = cart.itemCount || 0;
    return cart;
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

module.exports = { syncCartCount, qtyMap };
