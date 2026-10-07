const { get, post, put, del } = require("./request");

module.exports = {
  me: () => get("/api/v1/me"),
  units: () => get("/api/v1/meta/units"),
  store: () => get("/api/v1/store"),
  categories: () => get("/api/v1/categories"),
  products: (categoryIdOrOpts) => {
    let params = {};
    if (categoryIdOrOpts != null && typeof categoryIdOrOpts === "object") {
      params = categoryIdOrOpts;
    } else if (categoryIdOrOpts) {
      params = { categoryId: categoryIdOrOpts };
    }
    return get("/api/v1/products", params);
  },
  product: (id) => get(`/api/v1/products/${id}`),
  pickupSlots: () => get("/api/v1/pickup-slots"),
  restockWatch: (productId) => post(`/api/v1/products/${productId}/restock-watch`),
  restockStatus: (productId) => get(`/api/v1/products/${productId}/restock-watch`),
  restockCancel: (productId) => del(`/api/v1/products/${productId}/restock-watch`),

  cart: () => get("/api/v1/cart"),
  cartAdd: (productId, qty) => post("/api/v1/cart/items", { productId, qty }),
  cartSetQty: (productId, qty) => put(`/api/v1/cart/items/${productId}`, { qty }),
  cartRemove: (productId) => del(`/api/v1/cart/items/${productId}`),

  addresses: () => get("/api/v1/addresses"),
  address: (id) => get(`/api/v1/addresses/${id}`),
  addressCreate: (body) => post("/api/v1/addresses", body),
  addressUpdate: (id, body) => put(`/api/v1/addresses/${id}`, body),
  addressDefault: (id) => put(`/api/v1/addresses/${id}/default`),
  addressDelete: (id) => del(`/api/v1/addresses/${id}`),

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
  merchantCategoryCreate: (body) => post("/api/v1/merchant/categories", body),
  merchantCategoryUpdate: (id, body) => put(`/api/v1/merchant/categories/${id}`, body),
  merchantCategoryDelete: (id) => del(`/api/v1/merchant/categories/${id}`),
  merchantProducts: (categoryId) =>
    get("/api/v1/merchant/products", categoryId ? { categoryId } : {}),
  merchantProduct: (id) => get(`/api/v1/merchant/products/${id}`),
  merchantProductCreate: (body) => post("/api/v1/merchant/products", body),
  merchantProductUpdate: (id, body) => put(`/api/v1/merchant/products/${id}`, body),
  merchantOnShelf: (id, value) => put(`/api/v1/merchant/products/${id}/on-shelf`, { value }),
  merchantInStock: (id, value) => put(`/api/v1/merchant/products/${id}/in-stock`, { value }),
  merchantHomeRecommended: (id, value) =>
    put(`/api/v1/merchant/products/${id}/home-recommended`, { value }),
  merchantUpdateStore: (body) => put("/api/v1/merchant/store", body),
  merchantPickupSlots: () => get("/api/v1/merchant/pickup-slots"),
  merchantPickupSlotCreate: (body) => post("/api/v1/merchant/pickup-slots", body),
  merchantPickupSlotUpdate: (id, body) => put(`/api/v1/merchant/pickup-slots/${id}`, body),
  merchantPickupSlotDelete: (id) => del(`/api/v1/merchant/pickup-slots/${id}`),
  merchantInvite: () => get("/api/v1/merchant/invite"),
  merchantInviteRotate: () => post("/api/v1/merchant/invite/rotate"),
  acceptInvite: (code) => post("/api/v1/invites/accept", { code }),
};

