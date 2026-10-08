const api = require("../../../utils/api");
const copy = require("../../../utils/copy");
const { fenToYuan, yuanPlain } = require("../../../utils/money");
const { syncCartCount } = require("../../../utils/cartSync");
const { requestSubscribe } = require("../../../utils/subscribe");

const PHRASES = ["放门口", "少葱", "称重点", "不要电话"];

function splitSlotLabel(label) {
  const text = String(label || "").trim();
  const parts = text.match(/^(\S+)\s+(.+)$/);
  if (parts && /\d/.test(parts[2]) && /[-–~～]/.test(parts[2])) {
    return { day: parts[1], time: parts[2] };
  }
  return { day: "", time: text };
}

function deliveryView(fulfillment, totalFen, minDeliveryFen) {
  const min = Number(minDeliveryFen) || 0;
  const total = Number(totalFen) || 0;
  const active = fulfillment === "DELIVERY" && min > 0;
  const blocked = active && total < min;
  const deliveryMinText = active
    ? copy.format(copy.checkout.deliveryMin, { yuan: yuanPlain(min) })
    : "";
  const deliveryShortText = blocked
    ? copy.format(copy.checkout.deliveryShort, { yuan: yuanPlain(min - total) })
    : "";
  return {
    minDeliveryFen: min,
    deliveryMinText,
    deliveryShortText,
    deliveryBlocked: blocked,
  };
}

function faceLabel(fen) {
  const yuan = (Number(fen) || 0) / 100;
  if (Number.isInteger(yuan)) return String(yuan);
  return yuan.toFixed(2).replace(/0$/, "");
}

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
    couponSheet: false,
    couponTitle: "",
    couponSummary: "",
    couponSummaryKind: "none",
    discountYuan: "0.00",
    payYuan: "0.00",
    minDeliveryFen: 0,
    deliveryMinText: "",
    deliveryShortText: "",
    deliveryBlocked: false,
    storeClosed: false,
    submitting: false,
  },
  couponChosen: false,
  async onShow() {
    const gen = (this._loadGen || 0) + 1;
    this._loadGen = gen;
    const [cart, addresses, slots, mine, store] = await Promise.all([
      api.cart(),
      api.addresses(),
      api.pickupSlots(),
      api.couponsMine().catch(() => []),
      api.store().catch(() => ({})),
    ]);
    if (gen !== this._loadGen) return;
    const picked = getApp().globalData.pickedAddress;
    const address =
      picked || addresses.find((a) => a.isDefault) || addresses[0] || null;
    const totalFen = cart.totalFen || 0;
    const minDeliveryFen = Number(store && store.minDeliveryFen) || 0;
    const coupons = this.buildCoupons(mine, totalFen);
    const couponClaimId = this.pickCouponId(coupons);
    this.setData({
      totalFen,
      totalYuan: fenToYuan(totalFen),
      coupons,
      couponClaimId,
      address,
      slots: (slots || []).map((s) => ({ ...s, ...splitSlotLabel(s.label) })),
      pickupSlotId: this.data.pickupSlotId || (slots[0] && slots[0].id) || 0,
      ...this.payView(coupons, couponClaimId, totalFen),
      ...deliveryView(this.data.fulfillment, totalFen, minDeliveryFen),
      storeClosed:
        String((store && store.homeKicker) || "").trim() === copy.settings.kickerClosed,
    });
  },
  buildCoupons(mine, totalFen) {
    const coupons = (mine || [])
      .filter((c) => c.status === "UNUSED")
      .map((c) => {
        const minSpendFen = c.minSpendFen || 0;
        const eligible = totalFen >= minSpendFen;
        const faceFen = c.faceFen || 0;
        let ruleText = copy.checkout.noThreshold;
        if (!eligible) {
          ruleText = copy.format(copy.checkout.shortfall, { yuan: faceLabel(minSpendFen - totalFen) });
        } else if (minSpendFen > 0) {
          ruleText = copy.format(copy.coupons.minSpend, { yuan: faceLabel(minSpendFen) });
        }
        return {
          ...c,
          faceYuan: faceLabel(faceFen),
          eligible,
          discountFen: eligible ? Math.min(faceFen, totalFen) : 0,
          ruleText,
        };
      });
    coupons.sort((a, b) => {
      if (a.eligible !== b.eligible) return a.eligible ? -1 : 1;
      return b.discountFen - a.discountFen || a.id - b.id;
    });
    return coupons;
  },
  pickCouponId(coupons) {
    if (this.couponChosen) {
      const keep = coupons.find((c) => c.id === this.data.couponClaimId && c.eligible);
      return keep ? this.data.couponClaimId : 0;
    }
    const best = coupons.find((c) => c.eligible);
    return best ? best.id : 0;
  },
  payView(coupons, couponClaimId, totalFen) {
    const coupon = (coupons || []).find((c) => c.id === couponClaimId && c.eligible);
    const discountFen = coupon ? coupon.discountFen : 0;
    const eligibleCount = (coupons || []).filter((c) => c.eligible).length;
    let couponSummary = copy.checkout.noneEligible;
    let couponSummaryKind = "none";
    if (coupon) {
      couponSummary = `-¥${fenToYuan(discountFen)}`;
      couponSummaryKind = "off";
    } else if (eligibleCount) {
      couponSummary = copy.format(copy.checkout.available, { n: eligibleCount });
      couponSummaryKind = "avail";
    }
    return {
      discountYuan: fenToYuan(discountFen),
      payYuan: fenToYuan(Math.max(0, (totalFen || 0) - discountFen)),
      couponTitle: coupon ? coupon.title : "",
      couponSummary,
      couponSummaryKind,
    };
  },
  openCouponSheet() {
    if (!(this.data.coupons || []).length) return;
    this.setData({ couponSheet: true });
  },
  closeCouponSheet() {
    if (!this.data.couponSheet) return;
    this.setData({ couponSheet: false });
  },
  noop() {},
  pickCoupon(e) {
    const id = Number(e.currentTarget.dataset.id) || 0;
    const coupon = (this.data.coupons || []).find((c) => c.id === id);
    if (id && coupon && !coupon.eligible) {
      wx.showToast({ title: coupon.ruleText || copy.checkout.belowMin, icon: "none" });
      return;
    }
    this.couponChosen = true;
    this.setData({
      couponClaimId: id,
      couponSheet: false,
      ...this.payView(this.data.coupons, id, this.data.totalFen),
    });
  },
  switchF(e) {
    const fulfillment = e.currentTarget.dataset.v;
    this.setData({
      fulfillment,
      ...deliveryView(fulfillment, this.data.totalFen, this.data.minDeliveryFen),
    });
  },
  pickAddress() {
    wx.navigateTo({ url: "/packageCustomer/pages/address-list/address-list?pick=1" });
  },
  pickSlot(e) {
    this.setData({ pickupSlotId: Number(e.currentTarget.dataset.id) || 0 });
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
      if (this.data.deliveryBlocked) {
        wx.showToast({
          title: copy.format(copy.checkout.deliveryShort, {
            yuan: yuanPlain(this.data.minDeliveryFen - this.data.totalFen),
          }),
          icon: "none",
        });
        return;
      }
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
