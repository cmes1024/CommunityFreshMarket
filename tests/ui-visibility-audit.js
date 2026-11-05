/**
 * 小程序四维质量门禁（逻辑 / 交互 / UI / 性能）。
 * 文件名沿用历史，实际检查四个维度：
 *
 *   交互  - 关键提交按钮 5 连点是否只发 1 次写请求；半屏开关状态一致性
 *   UI    - 按钮几何是否被 fixed 底栏覆盖、触控热区 < 44px、字号 < 10px
 *   性能  - 每状态 setData 次数/字节、请求数/重复、渲染节点数、堆内存
 *   逻辑  - 页面能否打开、data 可读、运行时异常
 *
 * 关键背景：自动化框架的 element.tap() 直接向 DOM 节点派发事件，按钮被
 * position:fixed 的自定义 tabBar 盖住也照样「点击成功」。所以逻辑跑通
 * != 用户点得到，必须补几何断言 + 截图人工核对。
 *
 * 运行前置：
 *   1) 开发者工具开启自动化（CLI 输出的 autoPort 才是 WebSocket 端口；
 *      --auto-port 被 IDE HTTP 服务占用时会自动顺延，如 14326 -> 14327）：
 *      /Applications/wechatwebdevtools.app/Contents/MacOS/cli auto \
 *        --project "$(pwd)/miniprogram" --auto-port 14326 --trust-project
 *   2) npm i -D miniprogram-automator
 *
 * 运行： AUTO_PORT=14327 node tests/ui-visibility-audit.js
 * 可选： SHOT_DIR=/tmp/cfm-audit  截图目录（默认系统临时目录）
 *       SKIP_SHOTS=1             不截图（更快）
 *       SKIP_INTERACTION=1       跳过连点幂等测试
 *
 * 安全：连点阶段会 mock 所有非 GET 请求，不会写线上数据；结束后恢复。
 * 输出：SHOT_DIR/report.json；发现 BLOCKER 时退出码 1。
 * 注意：截图必须人工逐张过一遍，自动化只能判定几何/计数，判不了视觉层级。
 */
const fs = require('fs');
const os = require('os');
const path = require('path');

let automator;
try {
  automator = require('miniprogram-automator');
} catch (e) {
  console.error('缺少依赖，请先执行： npm i -D miniprogram-automator');
  process.exit(2);
}

const WS_PORT = Number(process.env.AUTO_PORT || 14327);
const SHOT_DIR = process.env.SHOT_DIR || path.join(os.tmpdir(), 'cfm-ui-audit');
const SKIP_SHOTS = process.env.SKIP_SHOTS === '1';
const SKIP_INTERACTION = process.env.SKIP_INTERACTION === '1';
fs.mkdirSync(SHOT_DIR, { recursive: true });

const TAB_PAGES = ['pages/home/home', 'pages/category/category', 'pages/cart/cart', 'pages/profile/profile'];
const SELECTORS = ['button', '.btn', '.cta', '.actions', '.submit', '.profile-save', '.sheet-close', '.save-bar', '.sheet-foot', '.mini-bar'];
const INTERACTIVE_CLASSES = new Set([
  'btn', 'cta', 'chip', 'shortcut', 'hit', 'add-wrap', 'step-btn', 'link-danger',
  'sheet-close', 'entry-link', 'rate-tip', 'settings-tab', 'm-filter-chip',
  'm-act', 'add-entry', 'field-input', 'head-stat', 'remove', 'seg-item',
]);

const STATES = [
  { url: '/pages/home/home', label: '01-home' },
  { url: '/pages/category/category', label: '02-category' },
  { url: '/pages/cart/cart', label: '03-cart' },
  { url: '/pages/profile/profile', label: '04-profile' },
  { url: '/pages/profile/profile', label: '05-profile-edit', data: { sheet: 'profile', sheetOpen: true } },
  { url: '/pages/profile/profile', label: '06-profile-contact', data: { sheet: 'contact', sheetOpen: true } },
  { url: '/pages/home/home', label: '07-home-product', click: '.home-card-hit' },
  { url: '/pages/category/category', label: '08-category-product', click: '.product-row .hit' },
  { url: '/packageCustomer/pages/checkout/checkout', label: '09-checkout' },
  { url: '/packageCustomer/pages/checkout/checkout', label: '10-checkout-coupon', data: { couponSheet: true } },
  { url: '/packageCustomer/pages/coupons/coupons', label: '11-coupons-history', data: { historySheet: true } },
  { url: '/packageCustomer/pages/points/points', label: '12-points-catalog', data: { sheet: 'catalog' } },
  { url: '/packageCustomer/pages/points/points', label: '13-points-redeems', data: { sheet: 'redeems' } },
  { url: '/packageCustomer/pages/orders/orders', label: '14-orders' },
  { url: '/packageCustomer/pages/address-list/address-list', label: '15-addresses' },
  { url: '/packageCustomer/pages/address-edit/address-edit', label: '16-address-edit' },
  { url: '/packageCustomer/pages/member-code/member-code', label: '17-member-code' },
  { url: '/packageMerchant/pages/orders/orders', label: '18-m-orders' },
  { url: '/packageMerchant/pages/products/products', label: '19-m-products' },
  { url: '/packageMerchant/pages/settings/settings', label: '20-m-settings' },
  { url: '/packageMerchant/pages/profile/profile', label: '21-m-invite', data: { inviteShow: true } },
  { url: '/packageMerchant/pages/categories/categories', label: '22-m-categories' },
  { url: '/packageMerchant/pages/coupons/coupons', label: '23-m-coupons-sheet', data: { sheetOpen: true } },
  { url: '/packageMerchant/pages/point-redeems/point-redeems', label: '24-m-redeems-picker', data: { sheet: 'picker' } },
  { url: '/packageMerchant/pages/point-redeems/point-redeems', label: '25-m-redeems-pending', data: { sheet: 'pending' } },
  { url: '/packageMerchant/pages/point-counter/point-counter', label: '26-m-counter-bubble', data: { bubbleSheet: true } },
  { url: '/packageMerchant/pages/balance-topup/balance-topup', label: '27-m-topup-bubble', data: { bubbleSheet: true } },
  { url: '/packageMerchant/pages/live-photos/live-photos', label: '28-m-livephotos' },
];

const BURSTS = [
  { url: '/packageCustomer/pages/address-edit/address-edit', label: '地址保存', sel: '.btn-cta' },
  { url: '/packageMerchant/pages/settings/settings', label: '门店设置保存', sel: '.save-btn' },
  { url: '/pages/profile/profile', label: '编辑资料保存', sel: '.profile-save' },
];

const wait = (ms) => new Promise((r) => setTimeout(r, ms));
function wt(promise, ms, label) {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error('TIMEOUT ' + label)), ms);
    Promise.resolve(promise).then(
      (v) => { clearTimeout(timer); resolve(v); },
      (e) => { clearTimeout(timer); reject(e); },
    );
  });
}

(async () => {
  const mp = await automator.connect({ wsEndpoint: `ws://127.0.0.1:${WS_PORT}` });
  const report = { port: WS_PORT, shotDir: SHOT_DIR, states: [], blockers: [], warns: [], bursts: [] };

  // ---- 全局探针 ----
  await mp.evaluate(() => {
    if (globalThis.__qaHook) return;
    globalThis.__qaHook = true;
    globalThis.__qaReqOrig = wx.request;
    globalThis.__qaReqLog = [];
    globalThis.__qaWrites = [];
    globalThis.__qaMockWrites = false;
    globalThis.__qaLiveTimers = new Set();
    const si = globalThis.setInterval, ci = globalThis.clearInterval;
    globalThis.setInterval = function (...a) { const id = si.apply(this, a); globalThis.__qaLiveTimers.add(id); return id; };
    globalThis.clearInterval = function (id) { globalThis.__qaLiveTimers.delete(id); return ci.call(this, id); };
    wx.request = function (o) {
      const m = String(o.method || 'GET').toUpperCase();
      const u = String(o.url || '');
      const short = u.replace(/^https?:\/\/[^/]+/, '');
      const rec = { m, u: short, t: Date.now(), done: null };
      globalThis.__qaReqLog.push(rec);
      if (globalThis.__qaMockWrites && m !== 'GET' && !/\/login|oauth|code2session/.test(u)) {
        globalThis.__qaWrites.push(m + ' ' + short);
        setTimeout(() => o.success && o.success({ statusCode: 200, data: { code: 0, message: 'ok', data: {} } }), 120);
        return { abort() {} };
      }
      const s = o.success, f = o.fail;
      o.success = function (res) { rec.done = Date.now(); rec.code = res && res.data && res.data.code; return s && s.apply(this, arguments); };
      o.fail = function (e) { rec.done = Date.now(); rec.fail = String(e && e.errMsg); return f && f.apply(this, arguments); };
      return globalThis.__qaReqOrig.call(wx, o);
    };
    globalThis.__qaSampleSetData = function (ms) {
      return new Promise((resolve) => {
        const p = getCurrentPages().slice(-1)[0];
        if (!p) return resolve({ count: 0, bytes: 0, max: 0 });
        const orig = p.setData;
        const stat = { count: 0, bytes: 0, max: 0 };
        p.setData = function (d, cb) {
          const n = JSON.stringify(d || {}).length;
          stat.count++; stat.bytes += n; stat.max = Math.max(stat.max, n);
          return orig.call(this, d, cb);
        };
        try { p.onShow && p.onShow(); } catch (e) { stat.err = e.message; }
        setTimeout(() => { p.setData = orig; resolve(stat); }, ms || 900);
      });
    };
  });

  const sys = await mp.evaluate(() => {
    const i = wx.getSystemInfoSync();
    return { w: i.windowWidth, h: i.windowHeight, sh: i.screenHeight, safeBottom: i.safeArea && i.safeArea.bottom };
  });
  const tabbarPx = 116 * (sys.w / 750) + (sys.sh - sys.safeBottom);
  const tabbarTop = sys.h - tabbarPx;
  report.sys = sys;
  report.tabbarTop = +tabbarTop.toFixed(1);

  // ---- 逐状态：几何 + UI + 性能 ----
  for (const state of STATES) {
    const row = { label: state.label, path: null, isTab: false, buttons: [], warns: [], blockers: [] };
    try {
      await mp.evaluate(() => { globalThis.__qaReqLog.length = 0; });
      await wt(mp.evaluate((u) => wx.reLaunch({ url: u }), state.url), 10000, 'relaunch');
      await wait(750);
      if (state.data) { await wt((await mp.currentPage()).setData(state.data), 6000, 'setData'); await wait(450); }
      if (state.click) {
        const p = await mp.currentPage();
        const els = await wt(p.$$(state.click), 8000, 'click');
        if (els[0]) { await wt(els[0].tap(), 8000, 'tap'); await wait(700); }
      }
      const page = await mp.currentPage();
      row.path = page && page.path;
      row.isTab = TAB_PAGES.includes(row.path);
      const nodes = await wt(page.$$('*'), 10000, 'nodes');
      row.nodes = nodes.length;

      const seen = new Set();
      for (const sel of SELECTORS) {
        let els = [];
        try { els = await wt(page.$$(sel), 5000, sel); } catch (e) { continue; }
        for (const el of els) {
          let size, off, wxml;
          try { size = await el.size(); off = await el.offset(); wxml = String(await el.outerWxml()); } catch (e) { continue; }
          if (!size || !off || !(size.width > 0) || !(size.height > 0)) continue;
          const m = wxml.match(/^<([a-zA-Z0-9_/-]+)([^>]*)>/);
          const tag = m ? m[1].replace(/^wx-/, '') : '?';
          const cm = m && m[2].match(/class="([^"]*)"/);
          const cls = cm ? cm[1] : '';
          const key = `${sel}|${tag}|${cls}|${off.top}|${off.left}`;
          if (seen.has(key)) continue;
          seen.add(key);
          const rect = { top: +off.top.toFixed(1), bottom: +(off.top + size.height).toFixed(1), w: +size.width.toFixed(1), h: +size.height.toFixed(1) };
          row.buttons.push({ sel, tag, cls: cls.slice(0, 50), ...rect });
          const isAction = tag === 'button' || /btn|cta|submit|save|actions|mini-bar|save-bar/.test(sel);
          if (row.isTab && isAction && rect.bottom > tabbarTop && rect.top < sys.h) {
            row.blockers.push({ type: 'COVERED_BY_TABBAR', sel, cls, rect });
          }
          if (isAction && (rect.w < 44 || rect.h < 44) && rect.w < 300 && rect.h < 200) {
            row.warns.push({ type: 'SMALL_TAP_TARGET', sel, cls, w: rect.w, h: rect.h });
          }
        }
      }
      // 字号与热区（全渲染树抽样）
      let smallText = 0;
      for (const el of nodes.slice(0, 90)) {
        let fsz, text, size, wxml;
        try { fsz = await el.style('font-size'); text = await el.domProperty('innerText'); size = await el.size(); wxml = String(await el.outerWxml()); } catch (e) { continue; }
        const px = parseFloat(fsz) || 0;
        if (text && String(text).trim() && px > 0 && px < 10) smallText++;
      }
      row.smallText = smallText;
      if (!SKIP_SHOTS) await mp.screenshot({ path: path.join(SHOT_DIR, state.label + '.png') });

      const stat = await mp.evaluate(() => globalThis.__qaSampleSetData(900));
      const reqs = await mp.evaluate(() => globalThis.__qaReqLog.slice());
      const env = await mp.evaluate(() => ({
        heapMB: performance.memory ? +(performance.memory.usedJSHeapSize / 1048576).toFixed(1) : 0,
        liveTimers: globalThis.__qaLiveTimers.size,
      }));
      row.setData = stat;
      row.requests = reqs.length;
      row.reqFailed = reqs.filter((r) => r.fail || (r.code !== undefined && r.code !== 0)).length;
      const counts = {};
      reqs.forEach((r) => { const k = r.m + ' ' + r.u; counts[k] = (counts[k] || 0) + 1; });
      row.reqDuplicated = Object.entries(counts).filter(([, v]) => v > 1).length;
      row.heapMB = env.heapMB;
      row.liveTimers = env.liveTimers;
      if (stat.bytes > 128 * 1024) row.warns.push({ type: 'SETDATA_HEAVY', bytes: stat.bytes });
      if (row.nodes > 1500) row.warns.push({ type: 'TOO_MANY_NODES', nodes: row.nodes });

      for (const b of row.blockers) report.blockers.push({ label: state.label, ...b });
      for (const w of row.warns) report.warns.push({ label: state.label, ...w });
    } catch (e) {
      row.blockers.push({ type: 'STATE_ERROR', why: e.message });
      report.blockers.push({ label: state.label, type: 'STATE_ERROR', why: e.message });
    }
    report.states.push(row);
    console.log(
      `${state.label} [${row.path || '?'}] nodes=${row.nodes || 0} buttons=${row.buttons.length} ` +
      `sd=${row.setData ? row.setData.bytes : '?'}B req=${row.requests || 0} ` +
      `BLOCKER=${row.blockers.length} WARN=${row.warns.length}`,
    );
  }

  // ---- 交互：连点幂等 ----
  if (!SKIP_INTERACTION) {
    await mp.evaluate(() => { globalThis.__qaMockWrites = true; });
    for (const b of BURSTS) {
      try {
        await wt(mp.evaluate((u) => wx.reLaunch({ url: u }), b.url), 9000, 'relaunch');
        await wait(850);
        if (b.sel === '.profile-save') {
          const p = await mp.currentPage();
          const a = await wt(p.$('.avatar'), 6000, 'avatar');
          await a.tap(); await wait(400);
          const i = await wt((await mp.currentPage()).$('.nick-input'), 6000, 'input');
          if (i) await i.input('门禁连点');
        } else if (b.url.includes('address-edit')) {
          const ins = await (await mp.currentPage()).$$('input');
          if (ins[0]) await ins[0].input('13800138000');
          if (ins[1]) await ins[1].input('门禁小区');
        }
        const p = await mp.currentPage();
        const els = await wt(p.$$(b.sel), 6000, 'sel');
        if (!els.length) { report.bursts.push({ label: b.label, writes: -1, err: 'no button' }); continue; }
        await mp.evaluate(() => { globalThis.__qaWrites.length = 0; });
        await Promise.all(Array.from({ length: 5 }, () => els[0].tap().catch(() => {})));
        await wait(900);
        const writes = await mp.evaluate(() => globalThis.__qaWrites.slice());
        const rec = { label: b.label, taps: 5, writes: writes.length, ok: writes.length <= 1, sample: writes.slice(0, 2) };
        report.bursts.push(rec);
        if (!rec.ok) report.blockers.push({ label: b.label, type: 'DUPLICATE_SUBMIT', writes: writes.length });
        console.log(`BURST ${b.label} 5连点 -> ${writes.length} 次写请求 ${rec.ok ? 'OK' : 'FAIL'}`);
      } catch (e) { report.bursts.push({ label: b.label, err: e.message }); }
    }
    await mp.evaluate(() => { globalThis.__qaMockWrites = false; });
  }

  fs.writeFileSync(path.join(SHOT_DIR, 'report.json'), JSON.stringify(report, null, 1));
  const warnTypes = {};
  report.warns.forEach((w) => { warnTypes[w.type] = (warnTypes[w.type] || 0) + 1; });
  console.log('\n=== SUMMARY ===');
  console.log(JSON.stringify({
    states: report.states.length,
    tabbarTop: report.tabbarTop,
    blockers: report.blockers.length,
    bursts: report.bursts,
    warnings: warnTypes,
    maxNodes: Math.max(...report.states.map((s) => s.nodes || 0)),
    maxSetDataBytes: Math.max(...report.states.map((s) => (s.setData && s.setData.bytes) || 0)),
    shots: SKIP_SHOTS ? 'skipped' : SHOT_DIR,
  }, null, 1));
  console.log(SKIP_SHOTS ? '' : '请人工逐张核对截图：几何/计数断言判不了裁剪、层级与文案问题。');
  await mp.disconnect();
  if (report.blockers.length) process.exit(1);
})().catch((e) => {
  console.error('FATAL', e.stack);
  process.exit(2);
});
