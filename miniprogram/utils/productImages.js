/** 解析商品图列表：优先 imageUrls，否则单张 imageUrl */
function productImages(p) {
  if (!p) return [];
  const urls = Array.isArray(p.imageUrls) ? p.imageUrls.filter(Boolean) : [];
  if (urls.length) return urls.slice(0, 3);
  return p.imageUrl ? [p.imageUrl] : [];
}

function productCover(p) {
  const urls = productImages(p);
  return urls[0] || "";
}

module.exports = { productImages, productCover };
