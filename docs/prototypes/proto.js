/* 绿意鲜生 UI 稿 v2 — 交互：篮状态、返回栈、结算反馈 */

const catalog = [
  { id: "bok", name: "本地小白菜", unit: "斤", price: 4.8, cat: "叶菜", thumb: "veg", sold: false, spec: "约 1 斤" },
  { id: "romaine", name: "罗马生菜", unit: "份", price: 6.5, cat: "叶菜", thumb: "veg", sold: false },
  { id: "carrot", name: "胡萝卜", unit: "斤", price: 3.2, cat: "根茎", thumb: "veg", sold: false },
  { id: "berry", name: "红颜草莓", unit: "份", price: 28, cat: "水果", thumb: "fruit", sold: true },
  { id: "egg", name: "土鸡蛋", unit: "份", price: 12.8, cat: "蛋奶", thumb: "veg", sold: false, spec: "约 10 枚" },
];

const cart = new Map(); // id -> qty
const history = [];
let mode = "customer";
let current = "c-home";
let activeCat = "叶菜";
let watching = false;
let arrived = false;

let memberCodeEntry = "PROFILE";

const customerTabs = [
  { id: "home", label: "首页", screen: "c-home", icon: "i-home" },
  { id: "category", label: "分类", screen: "c-category", icon: "i-grid" },
  { id: "cart", label: "商品篮", screen: "c-cart", icon: "i-basket" },
  { id: "profile", label: "我的", screen: "c-profile", icon: "i-user" },
];
const merchantTabs = [
  { id: "m-orders", label: "订单", screen: "m-orders", icon: "i-list", dot: true },
  { id: "m-products", label: "商品", screen: "m-products", icon: "i-box" },
  { id: "m-settings", label: "设置", screen: "m-settings", icon: "i-gear" },
  { id: "m-profile", label: "我的", screen: "m-profile", icon: "i-user" },
];

const screenMeta = {
  "c-home": { label: "首页", mode: "customer" },
  "c-category": { label: "分类 · 列表加购", mode: "customer" },
  "c-product": { label: "详情 · 多图轮播", mode: "customer" },
  "c-product-sold": { label: "售罄 · 到货提醒", mode: "customer" },
  "c-cart": { label: "商品篮", mode: "customer" },
  "c-checkout": { label: "结算三步 + 提交", mode: "customer" },
  "c-address": { label: "地址簿", mode: "customer" },
  "c-address-edit": { label: "编辑地址", mode: "customer" },
  "c-orders": { label: "我的订单", mode: "customer" },
  "c-order-detail": { label: "订单 · 横向状态", mode: "customer" },
  "c-order-done": { label: "再来一单", mode: "customer" },
  "c-order-pickup": { label: "我到店了", mode: "customer" },
  "c-profile": { label: "我的", mode: "customer" },
  "c-member-code": { label: "会员码", mode: "customer" },
  "m-orders": { label: "订单台 · 状态导航", mode: "merchant" },
  "m-order-detail": { label: "商家订单详情", mode: "merchant" },
  "m-products": { label: "商品 · 状态卡片", mode: "merchant" },
  "m-categories": { label: "分类管理", mode: "merchant" },
  "m-product-edit": { label: "编辑商品 · 最多3图", mode: "merchant" },
  "m-settings": { label: "门店设置", mode: "merchant" },
  "m-profile": { label: "商家我的", mode: "merchant" },
};

const $ = (sel, root = document) => root.querySelector(sel);
const $$ = (sel, root = document) => [...root.querySelectorAll(sel)];

function money(n) {
  return `¥${n.toFixed(2)}`;
}

function cartCount() {
  let n = 0;
  cart.forEach((q) => (n += q));
  return n;
}

function cartSum() {
  let s = 0;
  cart.forEach((q, id) => {
    const p = catalog.find((x) => x.id === id);
    if (p) s += p.price * q;
  });
  return s;
}

function addItem(id, delta = 1) {
  const p = catalog.find((x) => x.id === id);
  if (!p || p.sold) {
    showToast("已售罄");
    return;
  }
  const next = Math.max(0, (cart.get(id) || 0) + delta);
  if (next === 0) cart.delete(id);
  else cart.set(id, next);
  if (delta > 0) {
    showToast(`已加入 ${p.name}`);
    try {
      navigator.vibrate?.(10);
    } catch (_) {}
  }
  syncCartUI();
}

function askConfirm() {
  return new Promise((resolve) => {
    const layer = $("#confirmSheet");
    const okBtn = $("#confirmOk");
    const backBtn = $("#confirmBack");
    layer.hidden = false;
    const finish = (ok) => {
      layer.hidden = true;
      okBtn.removeEventListener("click", onOk);
      backBtn.removeEventListener("click", onBack);
      layer.removeEventListener("click", onBackdrop);
      resolve(ok);
    };
    const onOk = (ev) => {
      ev.stopPropagation();
      finish(true);
    };
    const onBack = (ev) => {
      ev.stopPropagation();
      finish(false);
    };
    const onBackdrop = (ev) => {
      if (ev.target === layer) finish(false);
    };
    okBtn.addEventListener("click", onOk);
    backBtn.addEventListener("click", onBack);
    layer.addEventListener("click", onBackdrop);
  });
}

function showToast(msg) {
  const el = $("#toast");
  el.textContent = msg;
  el.classList.add("show");
  clearTimeout(showToast._t);
  showToast._t = setTimeout(() => el.classList.remove("show"), 1600);
}

function syncCartUI() {
  const count = cartCount();
  const sum = cartSum();
  const badge = $("#navCartBadge");
  const mini = $("#miniCartBar");
  if (badge) {
    badge.hidden = count === 0;
    badge.textContent = String(count);
  }
  if (mini) {
    mini.hidden = !(current === "c-category" && count > 0);
    $("#miniCount").textContent = String(count);
    $("#miniSum").textContent = money(sum);
  }
  // tab cart badge via re-render
  renderTabs();
  renderCartPage();
  $("#checkoutSum") && ($("#checkoutSum").textContent = money(sum || 16.1));
  // steppers reflecting cart
  $$("[data-stepper]").forEach((el) => {
    const id = el.dataset.stepper;
    const q = cart.get(id) || (id === "bok" && current === "c-product" ? 1 : 0);
    const span = $("[data-qty]", el);
    if (span && current === "c-product" && id === "bok" && !cart.has(id)) span.textContent = "1";
    else if (span) span.textContent = String(cart.get(id) || 0);
  });
  // list qty buttons
  renderCategory();
}

function renderCategory() {
  const main = $("#catMain");
  if (!main) return;
  const list = catalog.filter((p) => p.cat === activeCat);
  main.innerHTML = list
    .map((p) => {
      const q = cart.get(p.id) || 0;
      return `
      <div class="product-row ${p.sold ? "is-sold" : ""}" data-product="${p.id}">
        <button type="button" class="product-hit" data-go="${p.sold ? "c-product-sold" : "c-product"}">
          <div class="thumb ${p.thumb}${p.sold ? " sold" : ""}"></div>
          <div class="meta">
            <p class="name">${p.name}</p>
            <p class="unit">${p.sold ? '<span class="chip sold">售罄</span>' : '<span class="chip">有货</span>'} · ${p.unit}${p.spec ? ` · ${p.spec}` : ""}</p>
            <div class="price">${money(p.price)} <small>/ ${p.unit}</small></div>
          </div>
        </button>
        ${
          p.sold
            ? `<button type="button" class="btn-watch" data-go="c-product-sold">提醒</button>`
            : q === 0
              ? `<button type="button" class="icon-add" data-add="${p.id}" aria-label="加购"><svg class="ico ico-sm"><use href="#i-plus"/></svg></button>`
              : `<div class="stepper compact" data-stepper-line="${p.id}">
                  <button type="button" data-line-step="${p.id}" data-delta="-1" aria-label="减"><svg class="ico ico-sm"><use href="#i-minus"/></svg></button>
                  <span>${q}</span>
                  <button type="button" data-line-step="${p.id}" data-delta="1" aria-label="加"><svg class="ico ico-sm"><use href="#i-plus"/></svg></button>
                </div>`
        }
      </div>`;
    })
    .join("");
}

function renderCartPage() {
  const empty = $("#cartEmpty");
  const filled = $("#cartFilled");
  const lines = $("#cartLines");
  if (!empty || !filled) return;
  if (cart.size === 0) {
    empty.hidden = false;
    filled.hidden = true;
    return;
  }
  empty.hidden = true;
  filled.hidden = false;
  lines.innerHTML = [...cart.entries()]
    .map(([id, q]) => {
      const p = catalog.find((x) => x.id === id);
      const hint = p.spec ? `${p.unit} · ${p.spec}` : p.unit;
      return `<div class="cart-row">
        <div class="cart-thumb" aria-hidden="true"></div>
        <div class="cart-meta">
          <div class="cart-name">${p.name}</div>
          <div class="price">${money(p.price)}<span class="price-hint"> / ${hint}</span></div>
          <button type="button" class="line-remove" data-remove="${id}">移除</button>
        </div>
        <div class="stepper">
          <button type="button" data-line-step="${id}" data-delta="-1" aria-label="减"><svg class="ico ico-sm"><use href="#i-minus"/></svg></button>
          <span>${q}</span>
          <button type="button" data-line-step="${id}" data-delta="1" aria-label="加"><svg class="ico ico-sm"><use href="#i-plus"/></svg></button>
        </div>
      </div>`;
    })
    .join("");
  $("#cartTotal").textContent = money(cartSum());
  const cartCountEl = $("#cartCount");
  if (cartCountEl) cartCountEl.textContent = String(cartCount());
}

function visibleCustomerTabs() {
  if (memberCodeEntry !== "TAB") return customerTabs;
  return [
    customerTabs[0],
    customerTabs[1],
    { id: "memberCode", label: "会员码", screen: "c-member-code", icon: "i-code", push: true },
    customerTabs[2],
    customerTabs[3],
  ];
}

function syncMemberCodeEntry() {
  const card = $("#protoMemberCode");
  if (card) card.hidden = memberCodeEntry === "TAB";
  $$("[data-member-entry]").forEach((btn) => {
    btn.classList.toggle("active", btn.dataset.memberEntry === memberCodeEntry);
  });
}

function renderScreenNav() {
  const nav = $("#screenNav");
  nav.innerHTML = "";
  Object.entries(screenMeta)
    .filter(([, m]) => m.mode === mode)
    .forEach(([id, m]) => {
      const li = document.createElement("li");
      const btn = document.createElement("button");
      btn.type = "button";
      btn.textContent = m.label;
      btn.className = id === current ? "active" : "";
      btn.addEventListener("click", () => go(id, { resetHistory: true }));
      li.appendChild(btn);
      nav.appendChild(li);
    });
}

function renderTabs() {
  const tabs = mode === "customer" ? visibleCustomerTabs() : merchantTabs;
  const screen = document.querySelector(`[data-screen="${current}"]`);
  const activeTab = screen?.dataset.tab;
  const tabbar = $("#tabbar");
  tabbar.innerHTML = "";
  tabs.forEach((t) => {
    const btn = document.createElement("button");
    btn.type = "button";
    btn.className = t.id === activeTab ? "active" : "";
    const countBadge =
      t.id === "cart" && cartCount() > 0
        ? `<span class="tab-badge">${cartCount()}</span>`
        : t.dot
          ? `<span class="dot"></span>`
          : "";
    btn.innerHTML = `<svg class="ico ico-sm" aria-hidden="true"><use href="#${t.icon}"/></svg><span>${t.label}</span>${countBadge}`;
    btn.addEventListener("click", () => {
      if (t.push) go(t.screen);
      else go(t.screen, { resetHistory: true });
    });
    tabbar.appendChild(btn);
  });
  tabbar.style.display = activeTab ? "flex" : "none";
}

function updateCheckoutSteps() {
  const hasFulfill = true;
  const hasRemark = ($("#remarkInput")?.value || "").trim().length > 0 || $$("#remarkPhrases .phrase.active").length > 0;
  $$("#checkoutSteps .step").forEach((el) => {
    const i = Number(el.dataset.stepI);
    el.classList.toggle("active", i === 1 || (i === 2 && hasRemark) || i === 3);
    el.classList.toggle("done", (i === 1 && hasFulfill) || (i === 2 && hasRemark));
  });
  // step 3 active when ready to submit
  const s3 = $('#checkoutSteps .step[data-step-i="3"]');
  if (s3) s3.classList.toggle("active", true);
}

function go(id, opts = {}) {
  const next = document.querySelector(`[data-screen="${id}"]`);
  if (!next) return;
  if (!opts.silent && current && current !== id && !opts.back && !opts.resetHistory) {
    history.push(current);
  }
  if (opts.resetHistory) history.length = 0;

  $$(".screen").forEach((s) => s.classList.remove("active"));
  next.classList.add("active");
  current = id;
  if (screenMeta[id]) mode = screenMeta[id].mode;

  $$(".mode-switch button").forEach((b) => b.classList.toggle("active", b.dataset.mode === mode));

  $("#navTitle").textContent = next.dataset.title || "";
  $("#topNav").classList.toggle("brand", next.hasAttribute("data-brand"));

  const canBack = history.length > 0 || next.hasAttribute("data-back");
  $("#navBack").hidden = !canBack;

  const toCustomer = next.hasAttribute("data-to-customer") && mode === "merchant";
  const navCustomer = $("#navCustomer");
  if (navCustomer) navCustomer.hidden = !toCustomer;
  $("#topNav").classList.toggle("has-customer", toCustomer);

  const showCart = next.hasAttribute("data-show-cart") && mode === "customer";
  $("#navCart").hidden = !showCart;

  renderTabs();
  syncMemberCodeEntry();
  renderScreenNav();
  $("#scroll").scrollTop = 0;
  syncCartUI();
  if (id === "c-checkout") updateCheckoutSteps();
  if (id === "c-order-pickup") {
    const btn = $("#arrivedBtn");
    const banner = $("#arriveBanner");
    if (arrived) {
      btn.textContent = "已通知店员";
      btn.disabled = true;
      banner.classList.add("done");
    }
  }
}

function goBack() {
  if (history.length) {
    const prev = history.pop();
    go(prev, { back: true, silent: true });
  } else {
    // fallback tab
    go(mode === "customer" ? "c-home" : "m-orders", { resetHistory: true });
  }
}

// —— events ——
$("#navBack").addEventListener("click", goBack);

document.addEventListener("click", (e) => {
  const entryBtn = e.target.closest("[data-member-entry]");
  if (entryBtn) {
    memberCodeEntry = entryBtn.dataset.memberEntry === "TAB" ? "TAB" : "PROFILE";
    syncMemberCodeEntry();
    renderTabs();
    return;
  }

  const sw = e.target.closest("[data-switch]");
  if (sw) {
    e.stopPropagation();
    const on = sw.classList.toggle("on");
    sw.setAttribute("aria-checked", on ? "true" : "false");
    showToast(on ? "已打开" : "已关闭");
    return;
  }

  const car = e.target.closest("[data-carousel]");
  if (car && (e.target.closest(".detail-carousel-track") || e.target.closest(".carousel-dots"))) {
    const track = $(".detail-carousel-track", car);
    const dots = $$(".carousel-dots .dot", car);
    const n = $$(".slide", track).length || 1;
    let i = Number(car.dataset.index || 0);
    i = (i + 1) % n;
    car.dataset.index = String(i);
    track.style.transform = `translateX(-${i * 100}%)`;
    dots.forEach((d, di) => d.classList.toggle("on", di === i));
    return;
  }

  const add = e.target.closest("[data-add]");
  if (add) {
    e.stopPropagation();
    e.preventDefault();
    addItem(add.dataset.add, 1);
    return;
  }

  const lineStep = e.target.closest("[data-line-step]");
  if (lineStep) {
    e.stopPropagation();
    addItem(lineStep.dataset.lineStep, Number(lineStep.dataset.delta));
    return;
  }

  const remove = e.target.closest("[data-remove]");
  if (remove) {
    cart.delete(remove.dataset.remove);
    syncCartUI();
    showToast("已移除");
    return;
  }

  const stepBtn = e.target.closest("[data-step]");
  if (stepBtn && stepBtn.closest("[data-stepper]")) {
    const box = stepBtn.closest("[data-stepper]");
    const span = $("[data-qty]", box);
    let q = Number(span.textContent) || 1;
    q = Math.max(1, q + Number(stepBtn.dataset.step));
    span.textContent = String(q);
    return;
  }

  const orderTab = e.target.closest("[data-order-tab]");
  if (orderTab) {
    const key = orderTab.dataset.orderTab;
    $$("[data-order-tab]").forEach((b) => {
      const on = b.dataset.orderTab === key;
      b.classList.toggle("active", on);
      b.setAttribute("aria-selected", on ? "true" : "false");
    });
    $$("[data-order-panel]").forEach((p) => {
      p.hidden = p.dataset.orderPanel !== key;
    });
    return;
  }

  const goEl = e.target.closest("[data-go]");
  if (goEl && !e.target.closest(".actions button, .icon-add, [data-line-step], [data-add], [data-accept], [data-advance]")) {
    if (goEl.dataset.modeSwitch) mode = goEl.dataset.modeSwitch;
    if (goEl.dataset.cat) activeCat = goEl.dataset.cat;
    if (goEl.dataset.toast) showToast(goEl.dataset.toast);
    go(goEl.dataset.go);
    if (goEl.dataset.cat) {
      $$("#catSide button").forEach((b) => b.classList.toggle("active", b.dataset.filter === activeCat));
      renderCategory();
    }
    return;
  }

  const cancelBtn = e.target.closest("[data-confirm-cancel]");
  if (cancelBtn) {
    e.stopPropagation();
    const toast = cancelBtn.dataset.toast || "已取消";
    askConfirm().then((ok) => {
      if (ok) showToast(toast);
    });
    return;
  }

  const toastOnly = e.target.closest("[data-toast]:not([data-go])");
  if (toastOnly && !toastOnly.closest(".actions")) {
    showToast(toastOnly.dataset.toast);
  }

  if (e.target.closest(".actions [data-toast]")) {
    e.stopPropagation();
    showToast(e.target.closest("[data-toast]").dataset.toast);
  }

  if (e.target.closest("[data-accept]")) {
    e.stopPropagation();
    showToast("已接单，进入备货");
    e.target.closest(".order-card")?.classList.add("accepted");
  }

  if (e.target.closest("[data-advance]")) {
    showToast("状态 → 备货中");
    $$(".h-steps li").forEach((li, i) => {
      li.classList.remove("current");
      if (i <= 1) li.classList.add("done");
      if (i === 1) li.classList.add("current");
    });
  }

  if (e.target.closest("[data-pick-addr]")) {
    $$("[data-pick-addr]").forEach((a) => a.classList.remove("selected"));
    e.target.closest("[data-pick-addr]").classList.add("selected");
    showToast("已选择地址");
    setTimeout(() => goBack(), 400);
  }
});

$("#catSide")?.addEventListener("click", (e) => {
  const btn = e.target.closest("[data-filter]");
  if (!btn) return;
  activeCat = btn.dataset.filter;
  $$("#catSide button").forEach((b) => b.classList.toggle("active", b === btn));
  renderCategory();
});

$("#detailAdd")?.addEventListener("click", () => {
  const q = Number($('[data-stepper="bok"] [data-qty]')?.textContent) || 1;
  const cur = cart.get("bok") || 0;
  cart.set("bok", cur + q);
  showToast(`已加入 ${q} 斤小白菜`);
  syncCartUI();
});

$("#watchRestock")?.addEventListener("click", () => {
  watching = true;
  const btn = $("#watchRestock");
  btn.textContent = "已订阅到货提醒";
  btn.disabled = true;
  btn.classList.add("disabled");
  showToast("有货时会通知你");
});

$("#arrivedBtn")?.addEventListener("click", () => {
  if (arrived) return;
  arrived = true;
  $("#arrivedBtn").textContent = "已通知店员";
  $("#arrivedBtn").disabled = true;
  $("#arriveBanner").classList.add("done");
  showToast("已通知店员：我到店了");
});

$("#reorderBtn")?.addEventListener("click", () => {
  addItem("egg", 1);
  go("c-cart", { resetHistory: true });
  showToast("有效商品已加入商品篮");
});

$("#saveAddr")?.addEventListener("click", () => {
  showToast("地址已保存");
  goBack();
});

$("#saveProduct")?.addEventListener("click", () => {
  showToast("商品已保存");
  go("m-products", { resetHistory: true });
});

$("#fulfillSeg")?.addEventListener("click", (e) => {
  const btn = e.target.closest("[data-fulfill]");
  if (!btn) return;
  $$("#fulfillSeg button").forEach((x) => x.classList.remove("active"));
  btn.classList.add("active");
  const delivery = btn.dataset.fulfill === "delivery";
  $("#deliveryBlock").hidden = !delivery;
  $("#pickupBlock").hidden = delivery;
  updateCheckoutSteps();
  renderRemarkPhrases();
});

$$("#pickupBlock .slot").forEach((slot) => {
  slot.addEventListener("click", () => {
    $$("#pickupBlock .slot").forEach((s) => s.classList.remove("active"));
    slot.classList.add("active");
  });
});

const deliveryRemarkPhrases = ["放门口", "少葱", "称重点", "不要电话"];
const pickupRemarkPhrases = ["少葱", "称重点"];
let settingsPhraseKind = "delivery";

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function activeRemarkPhrases() {
  return $("#pickupBlock") && !$("#pickupBlock").hidden ? pickupRemarkPhrases : deliveryRemarkPhrases;
}

function settingsPhraseList() {
  return settingsPhraseKind === "pickup" ? pickupRemarkPhrases : deliveryRemarkPhrases;
}

function phraseButtons(phrases) {
  return phrases
    .map(
      (text) =>
        `<button type="button" class="phrase" data-remove-phrase="${escapeHtml(text)}">${escapeHtml(text)} ×</button>`
    )
    .join("");
}

function renderRemarkPhrases() {
  const phrases = activeRemarkPhrases();
  const checkout = $("#remarkPhrases");
  if (checkout) {
    checkout.hidden = phrases.length === 0;
    checkout.innerHTML = phrases
      .map((text) => `<button type="button" class="phrase">${escapeHtml(text)}</button>`)
      .join("");
  }
  const settings = $("#settingsPhrases");
  if (settings) settings.innerHTML = phraseButtons(settingsPhraseList());
  const hint = $("#phraseHint");
  if (hint) {
    hint.textContent =
      settingsPhraseKind === "pickup"
        ? "顾客选自提时可以点。留空则自提只手写。每句最多 8 个字。"
        : "顾客选配送时可以点。留空则配送只手写。每句最多 8 个字。";
  }
  const draft = $("#phraseDraft");
  if (draft) draft.placeholder = settingsPhraseKind === "pickup" ? "如到店再称" : "如放门口";
  $$("#phraseNav .phrase-tab").forEach((btn) => {
    btn.classList.toggle("active", btn.dataset.phraseKind === settingsPhraseKind);
  });
  const input = $("#remarkInput");
  if (input) input.placeholder = phrases.length ? "点短语或手写补充" : "手写补充";
}

$("#settingsNav")?.addEventListener("click", (e) => {
  const btn = e.target.closest("[data-settings-tab]");
  if (!btn) return;
  const tab = btn.dataset.settingsTab;
  $$("#settingsNav .settings-tab").forEach((el) => el.classList.toggle("active", el === btn));
  $$("[data-settings-panel]").forEach((el) => {
    el.hidden = el.dataset.settingsPanel !== tab;
  });
});

$("#phraseNav")?.addEventListener("click", (e) => {
  const btn = e.target.closest("[data-phrase-kind]");
  if (!btn) return;
  settingsPhraseKind = btn.dataset.phraseKind === "pickup" ? "pickup" : "delivery";
  renderRemarkPhrases();
});

$("#settingsPhrases")?.addEventListener("click", (e) => {
  const btn = e.target.closest("[data-remove-phrase]");
  if (!btn) return;
  const phrases = settingsPhraseList();
  const text = btn.getAttribute("data-remove-phrase");
  const index = phrases.indexOf(text);
  if (index >= 0) phrases.splice(index, 1);
  renderRemarkPhrases();
});

$("#addPhrase")?.addEventListener("click", () => {
  const input = $("#phraseDraft");
  if (!input) return;
  const text = input.value.trim();
  if (!text) return;
  const phrases = settingsPhraseList();
  if ([...text].length > 8) {
    showToast("每句最多 8 个字");
    return;
  }
  if (phrases.includes(text)) {
    input.value = "";
    return;
  }
  if (phrases.length >= 8) {
    showToast("最多 8 句");
    return;
  }
  phrases.push(text);
  input.value = "";
  renderRemarkPhrases();
});

renderRemarkPhrases();

let protoThemePreset = "PRODUCE";

function applyProtoTheme(preset, primary, keepSlider) {
  if (!window.CfmTheme) return;
  protoThemePreset = preset;
  const palette = window.CfmTheme.resolve({ themePreset: preset, themePrimary: primary || "" });
  window.CfmTheme.pageStyle(palette).split(";").forEach((decl) => {
    const index = decl.indexOf(":");
    if (index < 1) return;
    document.documentElement.style.setProperty(decl.slice(0, index).trim(), decl.slice(index + 1).trim());
  });
  const custom = $("#themeCustom");
  if (custom) custom.hidden = preset !== "CUSTOM";
  const preview = $("#themePreview");
  if (preview) preview.style.background = palette.primary;
  const slider = $("#themeHue");
  if (slider && preset === "CUSTOM" && !keepSlider) {
    slider.value = String(window.CfmTheme.hueOf(palette.primary));
  }
  $$("[data-theme-preset]").forEach((btn) => btn.classList.toggle("on", btn.dataset.themePreset === preset));
}

function renderThemeOptions() {
  const box = $("#themeOptions");
  if (!box || !window.CfmTheme) return;
  box.innerHTML = window.CfmTheme.choices()
    .map((item) => {
      const dot = item.swatch
        ? `<span class="theme-dot" style="background:${item.swatch}"></span>`
        : `<span class="theme-dot rainbow"></span>`;
      const on = item.id === "PRODUCE" ? " on" : "";
      return `<button type="button" class="theme-opt${on}" data-theme-preset="${item.id}">${dot}<span>${item.name}</span></button>`;
    })
    .join("");
}

renderThemeOptions();
$("#themeOptions")?.addEventListener("click", (e) => {
  const btn = e.target.closest("[data-theme-preset]");
  if (!btn || !window.CfmTheme) return;
  const preset = btn.dataset.themePreset;
  if (preset === "CUSTOM") {
    const hue = protoThemePreset === "CUSTOM"
      ? Number($("#themeHue")?.value || 174)
      : window.CfmTheme.hueOf(window.CfmTheme.resolve({ themePreset: protoThemePreset }).primary);
    applyProtoTheme("CUSTOM", window.CfmTheme.primaryFromHue(hue));
    return;
  }
  applyProtoTheme(preset);
});
$("#themeHue")?.addEventListener("input", () => {
  if (!window.CfmTheme) return;
  applyProtoTheme("CUSTOM", window.CfmTheme.primaryFromHue(Number($("#themeHue").value)), true);
});

$("#remarkPhrases")?.addEventListener("click", (e) => {
  const btn = e.target.closest(".phrase");
  if (!btn) return;
  btn.classList.toggle("active");
  const parts = $$("#remarkPhrases .phrase.active").map((b) => b.textContent);
  $("#remarkInput").value = parts.join("；");
  updateCheckoutSteps();
});

$("#remarkInput")?.addEventListener("input", updateCheckoutSteps);

$("#submitOrder")?.addEventListener("click", async () => {
  const btn = $("#submitOrder");
  if (btn.disabled) return;
  if (cart.size === 0) {
    // demo allow submit with sample
  }
  btn.disabled = true;
  btn.classList.add("loading");
  btn.textContent = "提交中…";
  await new Promise((r) => setTimeout(r, 900));
  btn.disabled = false;
  btn.classList.remove("loading");
  btn.textContent = "提交预约";
  cart.clear();
  syncCartUI();
  $("#successSheet").hidden = false;
});

$("#sheetOk")?.addEventListener("click", () => {
  $("#successSheet").hidden = true;
  go("c-order-detail", { resetHistory: true });
});

$("#protoContactStore")?.addEventListener("click", () => {
  const sheet = $("#contactSheet");
  if (sheet) sheet.hidden = false;
});
$("#contactSheetClose")?.addEventListener("click", () => {
  const sheet = $("#contactSheet");
  if (sheet) sheet.hidden = true;
});
$("#contactPhoneBtn")?.addEventListener("click", () => {
  const sheet = $("#contactSheet");
  if (sheet) sheet.hidden = true;
});
$("#contactOnlineBtn")?.addEventListener("click", () => {
  const sheet = $("#contactSheet");
  if (sheet) sheet.hidden = true;
});

$("#sheetHome")?.addEventListener("click", () => {
  $("#successSheet").hidden = true;
  go("c-home", { resetHistory: true });
});

$$(".mode-switch button").forEach((b) => {
  b.addEventListener("click", () => {
    mode = b.dataset.mode;
    go(mode === "customer" ? "c-home" : "m-orders", { resetHistory: true });
  });
});

document.addEventListener("click", (e) => {
  const sw = e.target.closest(".switch");
  if (!sw) return;
  sw.classList.toggle("on");
  sw.setAttribute("aria-checked", sw.classList.contains("on") ? "true" : "false");
});

$$(".order-filter button").forEach((btn) => {
  btn.addEventListener("click", () => {
    $$(".order-filter button").forEach((b) => b.classList.remove("active"));
    btn.classList.add("active");
  });
});

renderCategory();
go("c-home", { resetHistory: true, silent: true });
