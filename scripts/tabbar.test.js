/**
 * 自定义 tabBar 高亮回归测试（node 原生 test，跑法：node scripts/tabbar.test.js）
 *
 * 小程序组件没法在 Node 里直接跑，这里把 Component / getCurrentPages / setData
 * / selectComponent 按微信的行为搭一个最小运行时，再驱动真实的组件定义，
 * 复现「switchTab 时页面栈还是上一个页面」导致高亮停在上一个 tab 的场景。
 */
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const test = require("node:test");

const ROOT = path.join(__dirname, "..");
const utils = require(path.join(ROOT, "miniprogram/utils/tabbar.js"));
const COMPONENT_FILE = path.join(ROOT, "miniprogram/custom-tab-bar/index.js");

function loadComponentDef() {
  let def = null;
  const prev = global.Component;
  global.Component = (d) => {
    def = d;
  };
  delete require.cache[require.resolve(COMPONENT_FILE)];
  require(COMPONENT_FILE);
  if (prev === undefined) delete global.Component;
  else global.Component = prev;
  return def;
}

/** 造一个 custom-tab-bar 实例，并把 <TabBar id="tabbar" current="..."> 的绑定模拟出来。 */
function createBar(stackRoute) {
  const def = loadComponentDef();
  const child = {
    data: { activeKey: "home" },
    setData(patch) {
      Object.assign(this.data, patch);
    },
  };
  const inst = Object.assign(
    {
      data: { ...def.data },
      setData(patch) {
        Object.assign(this.data, patch);
        // wxml 传 current，子组件 observers.current 收到后同步 activeKey
        if ("activeKey" in patch) {
          const v = patch.activeKey;
          if (v && v !== child.data.activeKey) child.setData({ activeKey: v });
        }
      },
      selectComponent(id) {
        return id === "#tabbar" ? child : null;
      },
    },
    def.methods
  );
  global.getCurrentPages = () => [{ route: stackRoute }];
  def.lifetimes.attached.call(inst);
  return { inst, child, def };
}

function pageOn(bar, key) {
  utils.syncTabBar({ getTabBar: () => bar }, key);
}

test("路由映射覆盖 app.json 里的 4 个 tab", () => {
  const app = JSON.parse(
    fs.readFileSync(path.join(ROOT, "miniprogram/app.json"), "utf8")
  );
  const paths = app.tabBar.list.map((item) => item.pagePath).sort();
  assert.deepEqual(paths, Object.keys(utils.KEY_BY_ROUTE).sort());
});

test("页面栈还停在上一个 tab 时，页面 onShow 仍能把高亮纠正到当前页", () => {
  // 复现真机现象：人在分类页，但 getCurrentPages() 栈顶还是首页
  const { inst, child, def } = createBar("pages/home/home");
  assert.equal(inst.data.activeKey, "home");

  pageOn(inst, "category");
  assert.equal(inst.data.activeKey, "category");
  assert.equal(child.data.activeKey, "category");

  // 组件自己的 pageLifetimes.show 随后触发，不能把高亮又猜回首页
  def.pageLifetimes.show.call(inst);
  assert.equal(inst.data.activeKey, "category");
  assert.equal(child.data.activeKey, "category");
});

test("4 个 tab 来回切，每次高亮都跟着页面走", () => {
  for (const [route, key] of Object.entries(utils.KEY_BY_ROUTE)) {
    const { inst, child } = createBar(route);
    pageOn(inst, key);
    assert.equal(inst.data.activeKey, key, route);
    assert.equal(child.data.activeKey, key, route);
  }
  const { inst, child } = createBar("pages/home/home");
  for (const key of ["category", "cart", "profile", "home", "category"]) {
    pageOn(inst, key);
    assert.equal(child.data.activeKey, key);
  }
});

test("点击时子组件乐观高亮后，父级同值也能把子组件拉回来", () => {
  const { inst, child } = createBar("pages/home/home");
  pageOn(inst, "category");
  child.setData({ activeKey: "cart" }); // TabBar.onTap 先亮目标项
  inst.setActive("category"); // 父级 activeKey 没变，current 的 observer 不会触发
  assert.equal(child.data.activeKey, "category");
});

test("没有自定义 tabBar 时不报错（商家分包页面也走这些工具）", () => {
  assert.doesNotThrow(() => utils.syncTabBar(null, "home"));
  assert.doesNotThrow(() => utils.syncTabBar({}, "home"));
  assert.doesNotThrow(() => utils.syncTabBar({ getTabBar: () => null }, "home"));
  assert.equal(utils.keyOfRoute("packageMerchant/pages/orders/orders"), "");
});

test("4 个 tab 页的 onShow 都显式声明了自己的激活项", () => {
  const expected = {
    "pages/home/home": "home",
    "pages/category/category": "category",
    "pages/cart/cart": "cart",
    "pages/profile/profile": "profile",
  };
  for (const [route, key] of Object.entries(expected)) {
    const src = fs.readFileSync(path.join(ROOT, "miniprogram", route + ".js"), "utf8");
    const call = 'syncTabBar(this, "' + key + '")';
    const onShowAt = src.search(/\bonShow\s*\(/);
    assert.ok(onShowAt >= 0, route + " 缺少 onShow");
    assert.ok(src.indexOf(call) > onShowAt, route + " 没在 onShow 里声明");
  }
});
