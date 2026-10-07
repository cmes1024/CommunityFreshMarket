const copy = require("./copy");

function trimNote(product) {
  const note = product && product.specNote;
  return note && String(note).trim() ? String(note).trim() : "";
}

/** 详情 / 半屏：单位：斤 · 约 1 斤 · 当面称重为准 */
function detailUnitLine(product) {
  const label = (product && product.unitLabel) || "";
  const parts = [`单位：${label}`];
  if (!product || !product.inStock) {
    parts.push(copy.product.soldOutToday);
    return parts.join(" · ");
  }
  const note = trimNote(product);
  if (note) parts.push(note);
  if (product.unit === "JIN") parts.push(copy.product.unitWeigh);
  return parts.join(" · ");
}

/** 首页 / 分类列表：斤 · 当面称重结 / 份 · 约 10 枚 */
function listUnitLine(product) {
  const label = (product && product.unitLabel) || "";
  if (!product || !product.inStock) {
    return label ? `${label} · ${copy.home.soldOutToday}` : copy.home.soldOutToday;
  }
  const note = trimNote(product);
  if (note) return `${label} · ${note}`;
  if (product.unit === "JIN") return `${label} · ${copy.home.weighHint}`;
  return label;
}

module.exports = {
  detailUnitLine,
  listUnitLine,
};
