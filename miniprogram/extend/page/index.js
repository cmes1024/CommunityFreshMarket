const copy = require("../../utils/copy");
const themeApply = require("../../utils/themeApply");
const hud = require("../../utils/hud");

const pageExtend = Page => {
  return object => {
    // 模板自带的那套 behaviors（formBehr / traitBehr / pageBehavior）已删除：
    // Page 构造器本来就不支持 behaviors，混进去不生效，只是每次注册多合并几个
    // 空 methods 与空 data；项目里也没有一处引用它们提供的方法。

    if (!object.data) {
      object.data = {};
    }
    // 文案统一入口；页面可在 data 里覆盖，一般直接用 {{copy.xxx}}
    if (object.data.copy == null) {
      object.data.copy = copy;
    }
    // 页面模板里没有一处用到胶囊 / 窗口 / 设备信息（Header 自己算胶囊位置），
    // 所以不再往每个页面的初始 data 里塞这几坨：那是每页 4 次同步原生调用 + 一份大对象，
    // 分包页第一次打开的那几百毫秒有一截就花在这上面
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

    const pageLoad = object.onLoad;
    object.onLoad = function () {
      hud.install(this);
      if (typeof pageLoad === "function") return pageLoad.apply(this, arguments);
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
