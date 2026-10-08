const api = require("../../../utils/api");
const copy = require("../../../utils/copy");
const { fenToYuan } = require("../../../utils/money");
const { syncCartCount } = require("../../../utils/cartSync");
const { requestSubscribe } = require("../../../utils/subscribe");

const PHRASES = ["放门口", "少葱", "称重点", "不要电话"];

Page({
  data: {
    fulfillment: "DELIVERY",
    address: null,
    slots: [],
    pickupSlotId: 0,
    phrases: PHRASES.map((text) => ({ text, on: false })),
    remark: "",
    totalFen: 0,
    totalYuan: "0.00",
    coupons: [],
    couponClaimId: 0,
    discountYuan: "0.00",
    payYuan: "0.00",
    submitting: false,
  },
  async onShow() {
    const [cart, addresses, slots, mine] = await Promise.all([
      api.cart(),
      api.addresses(),
      api.pickupSlots(),
      api.couponsMine().catch(() => []),
    ]);
    const picked = getApp().globalData.pickedAddress;
    const address =
      picked || addresses.find((a) => a.isDefault) || addresses[0] || null;
    const totalFen = cart.totalFen || 0;
    const coupons = (mine || [])
      .filter((c) => c.status === "UNUSED")
      .map((c) => ({
        ...c,
        faceYuan: fenToYuan(c.faceFen),
        eligible: totalFen >= (c.minSpendFen || 0),
      }));
    const still = coupons.some((c) => c.id === this.data.couponClaimId && c.eligible);
    this.setData({
      totalFen,
      totalYuan: fenToYuan(totalFen),
      coupons,
      couponClaimId: still ? this.data.couponClaimId : 0,
      address,
      slots,
      pickupSlotId: this.data.pickupSlotId || (slots[0] && slots[0].id) || 0,
    });
    this.refreshPay();
  },
  refreshPay() {
    const coupon = (this.data.coupons || []).find((c) => c.id === this.data.couponClaimId);
    let discountFen = 0;
    if (coupon && coupon.eligible) {
      discountFen = Math.min(coupon.faceFen || 0, this.data.totalFen || 0);
    }
    const payFen = Math.max(0, (this.data.totalFen || 0) - discountFen);
    this.setData({
      discountYuan: fenToYuan(discountFen),
      payYuan: fenToYuan(payFen),
    });
  },
  pickCoupon(e) {
    const id = Number(e.currentTarget.dataset.id) || 0;
    const coupon = (this.data.coupons || []).find((c) => c.id === id);
    if (id && coupon && !coupon.eligible) {
      wx.showToast({ title: copy.checkout.belowMin, icon: "none" });
      return;
    }
    this.setData({ couponClaimId: id });
    this.refreshPay();
  },
  switchF(e) {
    this.setData({ fulfillment: e.currentTarget.dataset.v });
  },
  pickAddress() {
    wx.navigateTo({ url: "/packageCustomer/pages/address-list/address-list?pick=1" });
  },
  pickSlot(e) {
    this.setData({ pickupSlotId: e.currentTarget.dataset.id });
  },
  togglePhrase(e) {
    const text = e.currentTarget.dataset.text;
    const phrases = this.data.phrases.map((p) => (p.text === text ? { ...p, on: !p.on } : p));
    const extra = phrases.filter((p) => p.on).map((p) => p.text).join("，");
    this.setData({ phrases, remark: extra });
  },
  onRemark(e) {
    this.setData({ remark: e.detail.value });
  },
  async submit() {
    if (this.data.submitting) return;
    const body = { fulfillment: this.data.fulfillment, remark: this.data.remark };
    if (this.data.fulfillment === "DELIVERY") {
      if (!this.data.address) {
        wx.showToast({ title: copy.checkout.pickAddress, icon: "none" });
        return;
      }
      body.addressId = this.data.address.id;
      body.asap = true;
    } else {
      if (!this.data.pickupSlotId) {
        wx.showToast({ title: copy.checkout.pickSlot, icon: "none" });
        return;
      }
      body.pickupSlotId = this.data.pickupSlotId;
    }
    if (this.data.couponClaimId) {
      body.couponClaimId = this.data.couponClaimId;
    }
    this.setData({ submitting: true });
    try {
      await requestSubscribe(["cancelTemplateId"]);
      const order = await api.submitOrder(body);
      await syncCartCount();
      wx.showToast({ title: copy.checkout.submittedToast, icon: "none" });
      wx.redirectTo({ url: `/packageCustomer/pages/order-detail/order-detail?id=${order.id}` });
    } catch (e) {
      this.setData({ submitting: false });
    }
  },
});
