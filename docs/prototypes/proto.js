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
  "c-product": { label: "详情 · 步进加购", mode: "customer" },
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
  "m-orders": { label: "订单台 · 马上处理", mode: "merchant" },
  "m-order-detail": { label: "商家订单详情", mode: "merchant" },
  "m-products": { label: "商品双开关", mode: "merchant" },
  "m-categories": { label: "分类管理", mode: "merchant" },
  "m-product-edit": { label: "编辑商品 · 单位/分类", mode: "merchant" },
  "m-settings": { label: "履约设置", mode: "merchant" },
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
      return `<div class="cell cart-line">
        <div class="cell-row">
          <div>
            <div style="font-weight:600">${p.name}</div>
            <div class="price">${money(p.price)} <small>/ ${p.unit}</small></div>
          </div>
          <div class="stepper">
            <button type="button" data-line-step="${id}" data-delta="-1" aria-label="减"><svg class="ico ico-sm"><use href="#i-minus"/></svg></button>
            <span>${q}</span>
            <button type="button" data-line-step="${id}" data-delta="1" aria-label="加"><svg class="ico ico-sm"><use href="#i-plus"/></svg></button>
          </div>
        </div>
        <button type="button" class="line-remove" data-remove="${id}">移除</button>
      </div>`;
    })
    .join("");
  $("#cartTotal").textContent = money(cartSum());
  const cartCountEl = $("#cartCount");
  if (cartCountEl) cartCountEl.textContent = String(cartCount());
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
  const tabs = mode === "customer" ? customerTabs : merchantTabs;
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
    btn.addEventListener("click", () => go(t.screen, { resetHistory: true }));
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

  const showCart = next.hasAttribute("data-show-cart") && mode === "customer";
  $("#navCart").hidden = !showCart;

  renderTabs();
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
});

$$("#pickupBlock .slot").forEach((slot) => {
  slot.addEventListener("click", () => {
    $$("#pickupBlock .slot").forEach((s) => s.classList.remove("active"));
    slot.classList.add("active");
  });
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
