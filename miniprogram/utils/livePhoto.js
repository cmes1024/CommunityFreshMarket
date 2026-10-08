const copy = require("./copy");
const { productImages } = require("./productImages");

function formatTaken(iso) {
  if (!iso) return "";
  const raw = String(iso).replace("T", " ");
  const d = new Date(raw.replace(/-/g, "/"));
  if (Number.isNaN(d.getTime())) return raw.slice(0, 16);
  const p = (n) => (n < 10 ? `0${n}` : String(n));
  const time = `${p(d.getHours())}:${p(d.getMinutes())}`;
  const now = new Date();
  const sameDay =
    d.getFullYear() === now.getFullYear() &&
    d.getMonth() === now.getMonth() &&
    d.getDate() === now.getDate();
  if (sameDay) return copy.livePhoto.takenToday.replace("{time}", time);
  const date = `${p(d.getMonth() + 1)}-${p(d.getDate())}`;
  return copy.livePhoto.takenOn.replace("{date}", date).replace("{time}", time);
}

function requestLabel(product) {
  const count = Number(product.livePhotoApplicantCount) || 0;
  if (product.livePhotoRequestedByMe) {
    return copy.livePhoto.requested.replace("{n}", String(count));
  }
  return copy.livePhoto.apply;
}

/** 详情/半屏图库。封面列表仍用 productImages，不把实景图提前。 */
function gallerySlides(product) {
  const catalog = productImages(product).map((url, i) => ({
    key: `c${i}`,
    kind: "catalog",
    url,
  }));
  if (!product || !product.livePhotoEnabled) return catalog;
  const label = requestLabel(product);
  if (!product.livePhotoUrl) {
    return [
      {
        key: "live",
        kind: "placeholder",
        showRequest: true,
        requestLabel: label,
      },
    ].concat(catalog);
  }
  return [
    {
      key: "live",
      kind: "live",
      url: product.livePhotoUrl,
      takenText: formatTaken(product.livePhotoTakenAt),
      showRequest: !!product.livePhotoExpired,
      requestLabel: label,
    },
  ].concat(catalog);
}

module.exports = { gallerySlides, formatTaken };
