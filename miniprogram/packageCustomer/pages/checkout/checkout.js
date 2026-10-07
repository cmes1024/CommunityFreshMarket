const api = require("../../../utils/api");
const copy = require("../../../utils/copy");
const { fenToYuan } = require("../../../utils/money");
const { syncCartCount } = require("../../../utils/cartSync");

const PHRASES = ["放门口", "少葱", "称重点", "不要电话"];

Page({
  data: {
    fulfillment: "DELIVERY",
    address: null,
    slots: [],
    pickupSlotId: 0,
    phrases: PHRASES.map((text) => ({ text, on: false })),
    remark: "",
    totalYuan: "0.00",
    submitting: false,
  },
  async onShow() {
    const [cart, addresses, slots] = await Promise.all([
      api.cart(),
      api.addresses(),
      api.pickupSlots(),
    ]);
    const picked = getApp().globalData.pickedAddress;
    const address =
      picked || addresses.find((a) => a.isDefault) || addresses[0] || null;
    this.setData({
      totalYuan: fenToYuan(cart.totalFen),
      address,
      slots,
      pickupSlotId: this.data.pickupSlotId || (slots[0] && slots[0].id) || 0,
    });
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
    this.setData({ submitting: true });
    try {
      const order = await api.submitOrder(body);
      await syncCartCount();
      wx.showToast({ title: copy.checkout.submittedToast, icon: "none" });
      wx.redirectTo({ url: `/packageCustomer/pages/order-detail/order-detail?id=${order.id}` });
    } catch (e) {
      this.setData({ submitting: false });
    }
  },
});
