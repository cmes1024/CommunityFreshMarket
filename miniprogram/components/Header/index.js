const { getMenuButtonInfo } = require("../../extend/functions/menuButton");
const { goTab } = require("../../utils/nav");

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
      value: "color:var(--color-text);font-weight:600;",
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
  },

  lifetimes: {
    attached() {
      // 初始 data 已算过一次，胶囊位置没变就不再 setData，少一次首屏渲染
      const next = buildHeaderMetrics();
      const cur = this.data.menuPositionInfo || {};
      if (
        next.headerHeightPx !== this.data.headerHeightPx ||
        next.menuPositionInfo.top !== cur.top ||
        next.menuPositionInfo.left !== cur.left
      ) {
        this.setData(next);
      }
    },
  },

  methods: {
    BackPage() {
      const pages = getCurrentPages();
      if (pages.length <= 1) {
        // 首页是 tabBar 页，只能 switchTab
        goTab("/pages/home/home");
      } else {
        wx.navigateBack();
      }
    },
    GoCustomer() {
      goTab("/pages/home/home");
    },
    currentPage() {
      const pages = getCurrentPages();
      return pages[pages.length - 1];
    },
  },
});
