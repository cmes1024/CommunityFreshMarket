const storeMode = require("./storeMode");

function productDetailMode(store) {
  return store && String(store.productDetailMode).toUpperCase() === "PAGE" ? "PAGE" : "SHEET";
}

/**
 * 打开商品详情。SHEET 模式下可带上列表里的商品对象作为预览，
 * 半屏先渲染已有数据，详情接口回来后再补齐图片 / 备注。
 * 橱窗形态没有加购，半屏只剩展示，直接进整页看说明与门店电话。
 */
function openProduct(page, id, store, preview) {
  const pid = Number(id);
  if (!pid) return;
  if (productDetailMode(store) === "PAGE" || storeMode.isShowcase(store)) {
    wx.navigateTo({ url: `/packageCustomer/pages/product/product?id=${pid}` });
    return;
  }
  const patch = { sheetProductId: pid, sheetShow: true };
  if (preview && Number(preview.id) === pid) patch.sheetPreview = preview;
  page.setData(patch);
}

module.exports = { productDetailMode, openProduct };
