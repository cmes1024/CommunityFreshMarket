const { get, post, put, del } = require("./request");
const themeApply = require("./themeApply");
const cache = require("./cache");

function withTheme(request) {
  return request.then((store) => {
    themeApply.applyStore(store);
    return store;
  });
}

/** 门店 / 分类 / 商品这类读多写少的数据缓存几秒，页面切回不必重新等网络 */
const TTL = {
  store: 60 * 1000,
  categories: 60 * 1000,
  products: 15 * 1000,
  product: 15 * 1000,
  pickupSlots: 60 * 1000,
  addresses: 60 * 1000,
  me: 30 * 1000,
  points: 20 * 1000,
  balance: 20 * 1000,
  couponsMine: 20 * 1000,
};

const inflight = new Map();

function cachedGet(key, path, data, ttl) {
  const hit = cache.read(key);
  if (hit != null) return Promise.resolve(hit);
  const running = inflight.get(key);
  if (running) return running;
  const task = get(path, data)
    .then((value) => cache.write(key, value, ttl))
    .finally(() => {
      if (inflight.get(key) === task) inflight.delete(key);
    });
  inflight.set(key, task);
  return task;
}

function productParams(categoryIdOrOpts) {
  if (categoryIdOrOpts != null && typeof categoryIdOrOpts === "object") return categoryIdOrOpts;
  if (categoryIdOrOpts) return { categoryId: categoryIdOrOpts };
  return {};
}

function productKey(params) {
  return "products:" + JSON.stringify(params || {});
}

/** 写操作成功后让相关缓存立即失效，商家改完顾客端不会看到旧数据 */
function afterSuccess(request, invalidate) {
  return request.then((value) => {
    invalidate();
    return value;
  });
}

function addressMutation(request) {
  return afterSuccess(request, () => cache.drop("addresses"));
}

function productMutation(request) {
  return afterSuccess(request, () => {
    cache.dropPrefix("products:");
    cache.dropPrefix("product:");
  });
}

function categoryMutation(request) {
  return afterSuccess(request, () => cache.drop("categories"));
}

function slotMutation(request) {
  return afterSuccess(request, () => cache.drop("pickupSlots"));
}

function storeMutation(request) {
  // 商家接口可能只回本次改动的字段，不能直接当门店缓存用，丢掉等下次拉全量
  return request.then((store) => {
    cache.drop("store");
    themeApply.applyStore(store);
    return store;
  });
}

module.exports = {
  me: () => cachedGet("me", "/api/v1/me", undefined, TTL.me),
  updateMe: (body) => afterSuccess(put("/api/v1/me", body), () => cache.drop("me")),
  units: () => get("/api/v1/meta/units"),
  store: () => withTheme(cachedGet("store", "/api/v1/store", undefined, TTL.store)),
  peekStore: () => cache.peek("store"),
  categories: () => cachedGet("categories", "/api/v1/categories", undefined, TTL.categories),
  peekCategories: () => cache.peek("categories"),
  products: (categoryIdOrOpts) => {
    const params = productParams(categoryIdOrOpts);
    return cachedGet(productKey(params), "/api/v1/products", params, TTL.products);
  },
  peekProducts: (categoryIdOrOpts) => cache.peek(productKey(productParams(categoryIdOrOpts))),
  product: (id) => cachedGet(`product:${id}`, `/api/v1/products/${id}`, undefined, TTL.product),
  /** 同步取已缓存的商品详情，详情页可先渲染再校准 */
  peekProduct: (id) => cache.peek(`product:${id}`),
  pickupSlots: () => cachedGet("pickupSlots", "/api/v1/pickup-slots", undefined, TTL.pickupSlots),
  peekPickupSlots: () => cache.peek("pickupSlots"),
  restockWatch: (productId) => post(`/api/v1/products/${productId}/restock-watch`),
  restockStatus: (productId) => get(`/api/v1/products/${productId}/restock-watch`),
  restockCancel: (productId) => del(`/api/v1/products/${productId}/restock-watch`),

  cart: () => get("/api/v1/cart"),
  cartAdd: (productId, qty) => post("/api/v1/cart/items", { productId, qty }),
  cartSetQty: (productId, qty) => put(`/api/v1/cart/items/${productId}`, { qty }),
  cartRemove: (productId) => del(`/api/v1/cart/items/${productId}`),

  addresses: () => cachedGet("addresses", "/api/v1/addresses", undefined, TTL.addresses),
  peekAddresses: () => cache.peek("addresses"),
  address: (id) => get(`/api/v1/addresses/${id}`),
  addressCreate: (body) => addressMutation(post("/api/v1/addresses", body)),
  addressUpdate: (id, body) => addressMutation(put(`/api/v1/addresses/${id}`, body)),
  addressDefault: (id) => addressMutation(put(`/api/v1/addresses/${id}/default`)),
  addressDelete: (id) => addressMutation(del(`/api/v1/addresses/${id}`)),

  submitOrder: (body) => post("/api/v1/orders", body),
  orders: (status) => get("/api/v1/orders", status ? { status } : {}),
  order: (id) => get(`/api/v1/orders/${id}`),
  cancelOrder: (id, reason) => post(`/api/v1/orders/${id}/cancel`, { reason }),
  arriveOrder: (id) => post(`/api/v1/orders/${id}/arrive`),
  reorder: (id) => post(`/api/v1/orders/${id}/reorder`),

  merchantOrders: () => get("/api/v1/merchant/orders"),
  merchantOrder: (id) => get(`/api/v1/merchant/orders/${id}`),
  merchantAdvance: (id, expectedStatus) =>
    post(`/api/v1/merchant/orders/${id}/advance`, { expectedStatus }),
  merchantCancelOrder: (id, reason) =>
    post(`/api/v1/merchant/orders/${id}/cancel`, { reason }),
  merchantCategories: () => get("/api/v1/merchant/categories"),
  merchantCategoryCreate: (body) =>
    categoryMutation(post("/api/v1/merchant/categories", body)),
  merchantCategoryUpdate: (id, body) =>
    categoryMutation(put(`/api/v1/merchant/categories/${id}`, body)),
  merchantCategoryDelete: (id) =>
    categoryMutation(del(`/api/v1/merchant/categories/${id}`)),
  merchantProducts: (categoryId) =>
    get("/api/v1/merchant/products", categoryId ? { categoryId } : {}),
  merchantProduct: (id) => get(`/api/v1/merchant/products/${id}`),
  merchantProductCreate: (body) =>
    productMutation(post("/api/v1/merchant/products", body)),
  merchantProductUpdate: (id, body) =>
    productMutation(put(`/api/v1/merchant/products/${id}`, body)),
  merchantOnShelf: (id, value) =>
    productMutation(put(`/api/v1/merchant/products/${id}/on-shelf`, { value })),
  merchantInStock: (id, value) =>
    productMutation(put(`/api/v1/merchant/products/${id}/in-stock`, { value })),
  merchantHomeRecommended: (id, value) =>
    productMutation(put(`/api/v1/merchant/products/${id}/home-recommended`, { value })),
  merchantProductDelete: (id) =>
    productMutation(del(`/api/v1/merchant/products/${id}`)),
  merchantProductBatch: (body) =>
    productMutation(post("/api/v1/merchant/products/batch", body)),
  merchantUpdateStore: (body) => storeMutation(put("/api/v1/merchant/store", body)),
  merchantPickupSlots: () => get("/api/v1/merchant/pickup-slots"),
  merchantPickupSlotCreate: (body) =>
    slotMutation(post("/api/v1/merchant/pickup-slots", body)),
  merchantPickupSlotUpdate: (id, body) =>
    slotMutation(put(`/api/v1/merchant/pickup-slots/${id}`, body)),
  merchantPickupSlotDelete: (id) =>
    slotMutation(del(`/api/v1/merchant/pickup-slots/${id}`)),
  merchantInvite: () => get("/api/v1/merchant/invite"),
  merchantInviteRotate: () => post("/api/v1/merchant/invite/rotate"),
  acceptInvite: (code) => post("/api/v1/invites/accept", { code }),

  pointsMe: () => cachedGet("points", "/api/v1/points/me", undefined, TTL.points),
  peekPoints: () => cache.peek("points"),
  pointsRedeem: (body) =>
    afterSuccess(post("/api/v1/points/redeem", body), () => {
      cache.drop("points");
      cache.drop("balance");
    }),
  memberCode: () => get("/api/v1/member-code"),

  merchantPointItems: () => get("/api/v1/merchant/points/items"),
  merchantPointItemCreate: (body) => post("/api/v1/merchant/points/items", body),
  merchantPointItemUpdate: (id, body) => put(`/api/v1/merchant/points/items/${id}`, body),
  merchantPointItemDelete: (id) => del(`/api/v1/merchant/points/items/${id}`),

  livePhotoRequest: (productId) => post(`/api/v1/products/${productId}/live-photo-requests`),

  coupons: () => get("/api/v1/coupons"),
  couponClaim: (id) =>
    afterSuccess(post(`/api/v1/coupons/${id}/claim`), () => cache.drop("couponsMine")),
  couponsMine: () =>
    cachedGet("couponsMine", "/api/v1/coupons/mine", undefined, TTL.couponsMine),
  peekCouponsMine: () => cache.peek("couponsMine"),

  checkin: (body) => afterSuccess(post("/api/v1/checkins", body || {}), () => cache.drop("points")),

  merchantPointRedeems: () => get("/api/v1/merchant/points/redeems"),
  merchantPointRedeemComplete: (id) => post(`/api/v1/merchant/points/redeems/${id}/complete`),
  merchantPointRedeemCancel: (id) => post(`/api/v1/merchant/points/redeems/${id}/cancel`),

  merchantLivePhotoRequests: () => get("/api/v1/merchant/live-photo-requests"),

  merchantCoupons: () => get("/api/v1/merchant/coupons"),
  merchantCouponCreate: (body) => post("/api/v1/merchant/coupons", body),
  merchantCouponUpdate: (id, body) => put(`/api/v1/merchant/coupons/${id}`, body),
  merchantCouponDelete: (id) => del(`/api/v1/merchant/coupons/${id}`),
  merchantCouponUnused: (userId, saleId) =>
    get("/api/v1/merchant/coupons/unused", saleId ? { userId, saleId } : { userId }),

  merchantCounterPoints: (body) =>
    afterSuccess(post("/api/v1/merchant/points/counter", body), () => {
      cache.drop("points");
      cache.drop("balance");
    }),
  merchantBalanceTopup: (body) =>
    afterSuccess(post("/api/v1/merchant/balance/topups", body), () => {
      cache.drop("balance");
      cache.drop("points");
    }),
  balanceMe: () => cachedGet("balance", "/api/v1/balance/me", undefined, TTL.balance),
  peekBalance: () => cache.peek("balance"),
  merchantCheckinsActive: () => get("/api/v1/merchant/checkins/active"),
};

