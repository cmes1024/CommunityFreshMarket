const api = require("../../../utils/api");
const copy = require("../../../utils/copy");
const { fenToYuan } = require("../../../utils/money");
const {
  advanceLabel,
  itemsLine,
  fulfillLine,
} = require("../../../utils/orderStatus");
const { requestSubscribe } = require("../../../utils/subscribe");

const TAB_DEFS = [
  { key: "urgent", label: copy.merchant.urgentTitle, empty: copy.merchant.urgentEmpty },
  { key: "PREPARING", label: copy.merchant.tabPreparing, empty: copy.merchant.emptyPreparing },
  { key: "DELIVERING", label: copy.merchant.tabDelivering, empty: copy.merchant.emptyDelivering },
  { key: "READY_PICKUP", label: copy.merchant.tabPickup, empty: copy.merchant.emptyPickup },
];

function stickyTopPx() {
  try {
    const menu = wx.getMenuButtonBoundingClientRect();
    return (Number(menu.top) || 0) + (Number(menu.height) || 32) + 6;
  } catch (e) {
    return 88;
  }
}

function mapOrder(o, isUrgent) {
  const arrived = !!o.customerArrivedAt;
  return {
    ...o,
    isUrgent: !!isUrgent,
    totalYuan: fenToYuan(o.totalAmountFen),
    itemsText: itemsLine(o.items),
    fulfillText: fulfillLine(o),
    primaryLabel: advanceLabel(o.status, o.fulfillment, { customerArrived: arrived }),
    chipLabel: arrived ? copy.merchant.arrivedChip : o.statusLabel,
    chipWarn: o.status === "PENDING_CONFIRM",
    chipPulse: arrived,
    pulse: arrived,
  };
}

function inTab(order, key) {
  if (key === "urgent") return !!order.isUrgent;
  return order.status === key;
}

function tabMeta(key) {
  return TAB_DEFS.find((t) => t.key === key) || TAB_DEFS[0];
}

Page({
  data: {
    stickyTop: 88,
    activeTab: "urgent",
    tabs: TAB_DEFS.map((t) => ({ key: t.key, label: t.label, count: 0 })),
    orders: [],
    visible: [],
    emptyText: copy.merchant.urgentEmpty,
    urgentCount: 0,
  },
  onLoad() {
    this.setData({ stickyTop: stickyTopPx() });
  },
  async onShow() {
    await this.reload();
  },
  /** 须用户点击触发；onShow 弹订阅框不可靠，未授权则服务端无法推新单 */
  async enableNotify() {
    await requestSubscribe(["newOrderTemplateId", "cancelTemplateId"]);
    wx.showToast({ title: copy.merchant.notifyDone, icon: "none" });
  },
  async reload() {
    const board = await api.merchantOrders();
    const urgent = (board.urgent || []).map((o) => mapOrder(o, true));
    const others = (board.others || []).map((o) => mapOrder(o, false));
    const orders = urgent.concat(others);
    const urgentCount = urgent.length;
    getApp().globalData.merchantUrgentCount = urgentCount;
    const tabs = TAB_DEFS.map((t) => ({
      key: t.key,
      label: t.label,
      count: orders.filter((o) => inTab(o, t.key)).length,
    }));
    const activeTab = tabMeta(this.data.activeTab).key;
    this.setData({
      orders,
      tabs,
      urgentCount,
      ...this.viewFor(orders, activeTab),
    });
  },
  viewFor(orders, activeTab) {
    return {
      activeTab,
      visible: orders.filter((o) => inTab(o, activeTab)),
      emptyText: tabMeta(activeTab).empty,
    };
  },
  onTab(e) {
    const key = e.currentTarget.dataset.key;
    if (!key || key === this.data.activeTab) return;
    this.setData(this.viewFor(this.data.orders, key));
    wx.pageScrollTo({ scrollTop: 0, duration: 0 });
  },
  go(e) {
    wx.navigateTo({
      url: `/packageMerchant/pages/order-detail/order-detail?id=${e.currentTarget.dataset.id}`,
    });
  },
  async onAdvance(e) {
    const { id, status } = e.currentTarget.dataset;
    try {
      await api.merchantAdvance(id, status);
      await this.reload();
    } catch (err) {}
  },
  async onCancel(e) {
    const { id } = e.currentTarget.dataset;
    const ok = await askCancel(id);
    if (!ok) return;
    try {
      await api.merchantCancelOrder(id, "商家取消");
      await this.reload();
    } catch (err) {}
  },
});

function askCancel(id) {
  return new Promise((resolve) => {
    wx.showModal({
      title: copy.merchant.cancelConfirmTitle,
      content: copy.format(copy.merchant.cancelConfirm, { id }),
      confirmText: copy.merchant.cancelConfirmOk,
      cancelText: copy.merchant.cancelConfirmBack,
      confirmColor: "#DC2626",
      success: (res) => resolve(!!res.confirm),
      fail: () => resolve(false),
    });
  });
}
