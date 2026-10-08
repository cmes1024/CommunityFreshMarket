/** 顾客端加购：effectiveMin/Max，缺省 min 0（列表）或 1（详情），max 999 */
function qtyBounds(product, opts) {
  const forDetail = opts && opts.forDetail;
  const p = product || {};
  const minRaw = p.effectiveMinQty != null ? p.effectiveMinQty : p.minQty;
  const maxRaw = p.effectiveMaxQty != null ? p.effectiveMaxQty : p.maxQty;
  let min =
    minRaw != null && minRaw !== "" && Number(minRaw) > 0
      ? Number(minRaw)
      : forDetail
        ? 1
        : 0;
  let max = maxRaw != null && maxRaw !== "" ? Number(maxRaw) : 999;
  if (!Number.isFinite(min)) min = forDetail ? 1 : 0;
  if (!Number.isFinite(max)) max = 999;
  if (max < min) max = min;
  return { min, max };
}

function initialDetailQty(product) {
  const { min, max } = qtyBounds(product, { forDetail: true });
  return Math.min(max, Math.max(1, min));
}

function normalizeCartQty(qty, minPurchase) {
  let n = Number(qty) || 0;
  if (n <= 0) return 0;
  const floor = minPurchase != null ? Number(minPurchase) : 1;
  if (Number.isFinite(floor) && floor > 0 && n < floor) n = floor;
  return n;
}

module.exports = { qtyBounds, initialDetailQty, normalizeCartQty };
