const hud = require("../../utils/hud");

const componentExtend = Component => {
  return object => {
    // 模板自带的那套 behaviors 已删除：它们只有空 properties/data 和几个无人
    // 调用的方法（getTraitNames / registerToRouter / agreement_yes …），
    // 却要让每个组件实例都合并一遍。列表里一屏几十个实例，纯属白付。

    if (!object.data) {
      object.data = {};
    }

    // 以前这里给每个组件实例注入 menuPositionInfo / windowInfo / deviceInfo /
    // systemSetting（4 次同步原生调用 + 一份大对象）。实际只有 Header 用，
    // 而 Header 自己算胶囊位置；Stepper 这类列表里成百个的组件又声明了
    // skipSystemData。商品列表一屏几十个 Stepper，注入一次就是几十次同步调用，
    // 现在整段去掉，组件需要什么自己取。
    delete object.skipSystemData;

    if (object.lifetimes && typeof object.lifetimes.attached === "function") {
      const origAttached = object.lifetimes.attached;
      object.lifetimes.attached = function () {
        hud.install(this);
        return origAttached.apply(this, arguments);
      };
    } else if (typeof object.attached === "function") {
      const origAttached = object.attached;
      object.attached = function () {
        hud.install(this);
        return origAttached.apply(this, arguments);
      };
    } else {
      object.lifetimes = object.lifetimes || {};
      object.lifetimes.attached = function () {
        hud.install(this);
      };
    }

    // 挂载登录结果函数到pageRouter上， 登录组件登录结果会触发改函数
    const componentsReady = object?.ready;
    object.ready = function(){
      componentsReady?.();
      this.pageRouter && object?.methods?.loginResultComponent && Object.assign(this.pageRouter, {
        loginResultComponent: object?.methods?.loginResultComponent.bind(this)
      });
    }

    return Component(object)
  }
}

const originComponent = Component;

Component = componentExtend(originComponent);
