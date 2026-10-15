const pageBehavior = require("../behaviors/pageBehavior");
const { getMenuButtonInfo, onMenuButtonInfoChange, offMenuButtonInfoChange } = require("../functions/menuButton");
const copy = require("../../utils/copy");
const themeApply = require("../../utils/themeApply");

const pageExtend = Page => {
  return object => {
    // 优先级!!!!!!
    // object > page > behaviors

    if(object.behaviors){
      object.behaviors.push(pageBehavior)
    }else {
      object.behaviors = [pageBehavior];
    }

    if (!object.data) {
      object.data = {};
    }
    // 文案统一入口；页面可在 data 里覆盖，一般直接用 {{copy.xxx}}
    if (object.data.copy == null) {
      object.data.copy = copy;
    }
    const menuPositionInfo = getMenuButtonInfo()
    object.data['menuPositionInfo'] = menuPositionInfo
    // 拆分为三个独立接口，避免一次拉取全部系统信息
    const windowInfo = wx.getWindowInfo?.() || {}
    const deviceInfo = wx.getDeviceInfo?.() || {}
    const systemSetting = wx.getSystemSetting?.() || {}
    object.data['windowInfo'] = windowInfo
    object.data['deviceInfo'] = deviceInfo
    object.data['systemSetting'] = systemSetting
    const themePack = themeApply.current();
    object.data.themePageStyle = themePack.pageStyle;
    object.data.themeBackground = themePack.background;

    const pageShow = object.onShow;
    object.onShow = function () {
      const pack = themeApply.current();
      const data = this.data || {};
      if (data.themePageStyle !== pack.pageStyle || data.themeBackground !== pack.background) {
        this.setData({ themePageStyle: pack.pageStyle, themeBackground: pack.background });
      }
      if (typeof pageShow === "function") return pageShow.apply(this, arguments);
    };

    // 挂载登录结果函数到pageRouter上， 登录组件登录结果会触发改函数
    const pageReady = object?.onReady;
    object.onReady = function(){
      pageReady?.();
      this.pageRouter && object.loginResultPage && Object.assign(this.pageRouter, {
        loginResultPage: object.loginResultPage.bind(this)
      });
    }

    return Page(object)
  }
}

const originPage = Page;

Page = pageExtend(originPage);