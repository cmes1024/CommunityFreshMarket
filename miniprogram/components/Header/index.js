const { getMenuButtonInfo } = require("../../extend/functions/menuButton");

function buildHeaderMetrics() {
  const menu = getMenuButtonInfo() || {};
  const top = Number(menu.top) || 0;
  const height = Number(menu.height) || 32;
  const left = Number(menu.left) || 0;
  const width = Number(menu.width) || 0;
  const bottomPad = 6; // px，胶囊下方留白
  const totalHeight = top + height + bottomPad;
  return {
    menuPositionInfo: {
      top,
      height,
      left,
      width,
      bottom: Number(menu.bottom) || top + height,
      right: Number(menu.right) || 0,
    },
    headerHeightPx: totalHeight,
    bottomDistance: bottomPad,
  };
}

Component({
  styleIsolation: "shared",
  properties: {
    back: {
      type: Boolean,
      value: true,
    },
    /** 商家 Tab 页左上角回顾客端 */
    toCustomer: {
      type: Boolean,
      value: false,
    },
    alignContent: {
      type: String,
      value: "center",
    },
    pageName: String,
    headerStyle: String,
    titleStyle: {
      type: String,
      value: "color:#134E4A;font-weight:600;",
    },
    brand: {
      type: Boolean,
      value: false,
    },
    /** 透明顶栏（英雄区沉浸）；需配合页面自行留白 */
    transparent: {
      type: Boolean,
      value: false,
    },
    zIndex: {
      type: Number,
      value: 100,
    },
  },

  observers: {
    alignContent(v) {
      this.setData({ isTitleCenter: v !== "left" && v !== "right" });
    },
  },

  data: {
    ...buildHeaderMetrics(),
    isTitleCenter: true,
    proxyConfig: {
      themeColor: "#0F766E",
      policy: {
        title: "隐私协议",
        desc: ["欢迎您使用小程序，请您仔细阅读并充分理解"],
        list: [
          {
            agreementName: "《隐私政策》",
            link: "",
          },
        ],
      },
      btns: {
        disagreenText: "不同意并退出",
      },
    },
  },

  lifetimes: {
    attached() {
      this.setData(buildHeaderMetrics());
    },
  },

  methods: {
    BackPage() {
      const pages = getCurrentPages();
      if (pages.length <= 1) {
        wx.reLaunch({ url: "/packageCustomer/pages/home/home" });
      } else {
        wx.navigateBack();
      }
    },
    GoCustomer() {
      wx.reLaunch({ url: "/packageCustomer/pages/home/home" });
    },
    currentPage() {
      const pages = getCurrentPages();
      return pages[pages.length - 1];
    },
    agree() {
      this.currentPage()?.agreement_yes?.();
    },
    disagree() {
      this.currentPage()?.agreement_no?.();
    },
    agreementClick() {},
  },
});
