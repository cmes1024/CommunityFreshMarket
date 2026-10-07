function productDetailMode(store) {
  return store && String(store.productDetailMode).toUpperCase() === "PAGE" ? "PAGE" : "SHEET";
}

function openProduct(page, id, store) {
  const pid = Number(id);
  if (!pid) return;
  if (productDetailMode(store) === "PAGE") {
    wx.navigateTo({ url: `/packageCustomer/pages/product/product?id=${pid}` });
    return;
  }
  page.setData({ sheetProductId: pid, sheetShow: true });
}

module.exports = { productDetailMode, openProduct };
