const CUSTOMER = [
  {
    key: "home",
    label: "首页",
    url: "/packageCustomer/pages/home/home",
    icon: "/images/tab-home.svg",
    iconActive: "/images/tab-home-active.svg",
  },
  {
    key: "category",
    label: "分类",
    url: "/packageCustomer/pages/category/category",
    icon: "/images/tab-grid.svg",
    iconActive: "/images/tab-grid-active.svg",
  },
  {
    key: "cart",
    label: "商品篮",
    url: "/packageCustomer/pages/cart/cart",
    icon: "/images/tab-basket.svg",
    iconActive: "/images/tab-basket-active.svg",
  },
  {
    key: "profile",
    label: "我的",
    url: "/packageCustomer/pages/profile/profile",
    icon: "/images/tab-user.svg",
    iconActive: "/images/tab-user-active.svg",
  },
];

const MERCHANT = [
  {
    key: "orders",
    label: "订单",
    url: "/packageMerchant/pages/orders/orders",
    icon: "/images/tab-list.svg",
    iconActive: "/images/tab-list-active.svg",
  },
  {
    key: "products",
    label: "商品",
    url: "/packageMerchant/pages/products/products",
    icon: "/images/tab-box.svg",
    iconActive: "/images/tab-box-active.svg",
  },
  {
    key: "settings",
    label: "设置",
    url: "/packageMerchant/pages/settings/settings",
    icon: "/images/tab-gear.svg",
    iconActive: "/images/tab-gear-active.svg",
  },
  {
    key: "profile",
    label: "我的",
    url: "/packageMerchant/pages/profile/profile",
    icon: "/images/tab-user.svg",
    iconActive: "/images/tab-user-active.svg",
  },
];

function buildTabs(mode) {
  const app = getApp();
  const cartCount = (app.globalData && app.globalData.cartCount) || 0;
  const urgent = (app.globalData && app.globalData.merchantUrgentCount) || 0;
  return (mode === "merchant" ? MERCHANT : CUSTOMER).map((t) => {
    let badge = 0;
    if (t.key === "cart") badge = cartCount;
    if (t.key === "orders") badge = urgent;
    return { ...t, badge };
  });
}

Component({
  properties: {
    current: { type: String, value: "home" },
    mode: { type: String, value: "customer" },
  },
  data: { tabs: CUSTOMER },
  observers: {
    mode() {
      this.setData({ tabs: buildTabs(this.data.mode) });
    },
  },
  pageLifetimes: {
    show() {
      this.setData({ tabs: buildTabs(this.data.mode) });
    },
  },
  attached() {
    this.setData({ tabs: buildTabs(this.data.mode) });
  },
  methods: {
    onTap(e) {
      const url = e.currentTarget.dataset.url;
      if (!url) return;
      wx.reLaunch({ url });
    },
  },
});
