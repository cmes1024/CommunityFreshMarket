/**
 * 门店主题。青绿是手调色板；赭石 / 绛红 / 海蓝与自定义主色走同一套生成规则。
 * 色相分别偏向生鲜、早点、饭馆、商超，按钮上只写颜色名。
 * 价格、待确认、售罄不在这里，继续用 app.wxss 里的语义色。
 */
(function (root, factory) {
  const api = factory();
  if (typeof module === "object" && module.exports) module.exports = api;
  if (root) root.CfmTheme = api;
})(typeof globalThis !== "undefined" ? globalThis : this, function () {
  const PRESET_HUE = { BREAKFAST: 18, EATERY: 356, MARKET: 210 };

  const PRODUCE = {
    id: "PRODUCE",
    primary: "#0f766e",
    pressed: "#115e59",
    secondary: "#0d9488",
    onPrimary: "#ffffff",
    onInk: "#f0fdfa",
    background: "#f0fdfa",
    mist: "#ccfbf1",
    line: "#cce5e1",
    ink: "#0f3d38",
    text: "#134e4a",
    muted: "#5b7c78",
    phVeg: "#c5e8e1",
    phDeep: "#b7d4ce",
    scrim: "rgba(19, 78, 74, 0.5)",
    shadowFloat: "0 16rpx 48rpx rgba(15, 118, 110, 0.16)",
    shadowLift: "0 16rpx 48rpx rgba(15, 118, 110, 0.2)",
    mainOpacity: "rgba(15, 118, 110, 0.09)",
    heroTop: "rgba(15, 61, 56, 0.28)",
    heroBottom: "rgba(15, 61, 56, 0.55)",
    cardTop: "rgba(19, 78, 74, 0.08)",
    cardMid: "rgba(19, 78, 74, 0.28)",
    cardBottom: "rgba(19, 78, 74, 0.78)",
    checkBg: "rgba(19, 78, 74, 0.35)",
  };

  function clamp(n, min, max) {
    return Math.min(max, Math.max(min, n));
  }

  function hexToRgb(hex) {
    const h = hex.replace("#", "");
    return [parseInt(h.slice(0, 2), 16), parseInt(h.slice(2, 4), 16), parseInt(h.slice(4, 6), 16)];
  }

  function rgbToHex(r, g, b) {
    const c = (n) => clamp(Math.round(n), 0, 255).toString(16).padStart(2, "0");
    return "#" + c(r) + c(g) + c(b);
  }

  function rgbToHsl(r, g, b) {
    r /= 255;
    g /= 255;
    b /= 255;
    const max = Math.max(r, g, b);
    const min = Math.min(r, g, b);
    const l = (max + min) / 2;
    let h = 0;
    let s = 0;
    const d = max - min;
    if (d !== 0) {
      s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
      switch (max) {
        case r:
          h = (g - b) / d + (g < b ? 6 : 0);
          break;
        case g:
          h = (b - r) / d + 2;
          break;
        default:
          h = (r - g) / d + 4;
          break;
      }
      h /= 6;
    }
    return [h * 360, s * 100, l * 100];
  }

  function hslToRgb(h, s, l) {
    h = ((h % 360) + 360) % 360;
    s = clamp(s, 0, 100) / 100;
    l = clamp(l, 0, 100) / 100;
    const a = s * Math.min(l, 1 - l);
    const f = (n) => {
      const k = (n + h / 30) % 12;
      return l - a * Math.max(Math.min(k - 3, 9 - k, 1), -1);
    };
    return [f(0) * 255, f(8) * 255, f(4) * 255];
  }

  function hslHex(h, s, l) {
    const rgb = hslToRgb(h, s, l);
    return rgbToHex(rgb[0], rgb[1], rgb[2]);
  }

  function channel(c) {
    const x = c / 255;
    return x <= 0.04045 ? x / 12.92 : Math.pow((x + 0.055) / 1.055, 2.4);
  }

  function luminance(hex) {
    const [r, g, b] = hexToRgb(hex);
    return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b);
  }

  function contrast(a, b) {
    const hi = Math.max(luminance(a), luminance(b));
    const lo = Math.min(luminance(a), luminance(b));
    return (hi + 0.05) / (lo + 0.05);
  }

  function rgba(hex, alpha) {
    const [r, g, b] = hexToRgb(hex);
    return "rgba(" + r + ", " + g + ", " + b + ", " + alpha + ")";
  }

  function normalizeHex(raw) {
    const text = String(raw || "").trim();
    const match = /^#?([0-9a-fA-F]{6})$/.exec(text);
    return match ? "#" + match[1].toLowerCase() : "";
  }

  function readPreset(raw) {
    const value = String(raw || "").trim().toUpperCase();
    if (value === "TEAL" || value === "PRODUCE") return "PRODUCE";
    if (value === "AMBER" || value === "BREAKFAST") return "BREAKFAST";
    if (value === "BRICK" || value === "EATERY") return "EATERY";
    if (value === "INK" || value === "MARKET") return "MARKET";
    if (value === "CUSTOM") return "CUSTOM";
    return "PRODUCE";
  }

  function hueOf(hex) {
    const [h, s] = rgbToHsl.apply(null, hexToRgb(hex));
    return s < 8 ? 174 : Math.round(h) % 360;
  }

  /** 预设仍用这档深度，绛红因此深于售罄红。 */
  function presetPrimary(hue) {
    const h = ((Number(hue) % 360) + 360) % 360;
    const s = 62;
    let l = 34;
    let current = hslHex(h, s, l);
    let guard = 0;
    while (contrast(current, "#ffffff") < 4.6 && l > 16 && guard++ < 24) {
      l -= 1;
      current = hslHex(h, s, l);
    }
    guard = 0;
    while (contrast(current, "#ffffff") > 6.6 && l < 40 && guard++ < 24) {
      const next = hslHex(h, s, l + 1);
      if (contrast(next, "#ffffff") < 4.6) break;
      l += 1;
      current = next;
    }
    return current;
  }

  /**
   * 自定义只保留色相。饱和度拉到 84%，明度放到白字对比度仍不少于 4.6 的最亮档，上限 64%。
   * 正黄仍会偏深，否则白字按钮看不清。
   */
  function vividPrimary(hue) {
    const h = ((Number(hue) % 360) + 360) % 360;
    const s = 84;
    let l = 36;
    let current = hslHex(h, s, l);
    let guard = 0;
    while (contrast(current, "#ffffff") < 4.6 && l > 16 && guard++ < 40) {
      l -= 1;
      current = hslHex(h, s, l);
    }
    guard = 0;
    while (contrast(current, "#ffffff") > 5.2 && l < 64 && guard++ < 40) {
      const next = hslHex(h, s, l + 1);
      if (contrast(next, "#ffffff") < 4.6) break;
      l += 1;
      current = next;
    }
    return current;
  }

  function canonicalPrimary(hex) {
    const normalized = normalizeHex(hex);
    if (!normalized) return PRODUCE.primary;
    const [h, s] = rgbToHsl.apply(null, hexToRgb(normalized));
    if (s >= 80 && s <= 90 && contrast(normalized, "#ffffff") >= 4.6) return normalized;
    return vividPrimary(s < 12 ? 174 : h);
  }

  function primaryFromHue(hue) {
    const h = ((Number(hue) % 360) + 360) % 360;
    return vividPrimary(h);
  }

  function tuneAgainst(hexHue, sat, startL, bg, min) {
    let l = startL;
    let hex = hslHex(hexHue, sat, l);
    let guard = 0;
    while (contrast(hex, bg) < min && l > 10 && guard++ < 24) {
      l -= 1;
      hex = hslHex(hexHue, sat, l);
    }
    return hex;
  }

  function paletteFromPrimary(primary, id) {
    const [h] = rgbToHsl.apply(null, hexToRgb(primary));
    const pressed = tuneAgainst(h, 58, 22, "#ffffff", 5.5);
    const secondary = hslHex(h, 70, 36);
    const background = hslHex(h, 72, 97);
    const mist = hslHex(h, 68, 90);
    const line = hslHex(h, 24, 85);
    const ink = tuneAgainst(h, 58, 16, background, 10);
    const text = tuneAgainst(h, 52, 20, "#ffffff", 8);
    const muted = tuneAgainst(h, 16, 40, "#ffffff", 4.6);
    const onInk = background;
    return {
      id: id,
      primary: primary,
      pressed: contrast(pressed, primary) < 1.15 ? hslHex(h, 58, 18) : pressed,
      secondary: secondary,
      onPrimary: "#ffffff",
      onInk: onInk,
      background: background,
      mist: mist,
      line: line,
      ink: ink,
      text: text,
      muted: muted,
      phVeg: hslHex(h, 38, 84),
      phDeep: hslHex(h, 24, 77),
      scrim: rgba(text, 0.5),
      shadowFloat: "0 16rpx 48rpx " + rgba(primary, 0.16),
      shadowLift: "0 16rpx 48rpx " + rgba(primary, 0.2),
      mainOpacity: rgba(primary, 0.09),
      heroTop: rgba(ink, 0.28),
      heroBottom: rgba(ink, 0.55),
      cardTop: rgba(text, 0.08),
      cardMid: rgba(text, 0.28),
      cardBottom: rgba(text, 0.78),
      checkBg: rgba(text, 0.35),
    };
  }

  function resolve(store) {
    const preset = readPreset(store && store.themePreset);
    if (preset === "PRODUCE") return Object.assign({}, PRODUCE);
    if (preset === "CUSTOM") {
      const hex = normalizeHex(store && store.themePrimary);
      if (!hex) return Object.assign({}, PRODUCE);
      return paletteFromPrimary(canonicalPrimary(hex), "CUSTOM");
    }
    return paletteFromPrimary(presetPrimary(PRESET_HUE[preset]), preset);
  }

  function pageStyle(palette) {
    const p = palette || PRODUCE;
    const decl = [
      ["--color-primary", p.primary],
      ["--color-primary-pressed", p.pressed],
      ["--color-secondary", p.secondary],
      ["--color-cta", p.primary],
      ["--color-on-primary", p.onPrimary],
      ["--color-on-ink", p.onInk],
      ["--color-background", p.background],
      ["--color-mist", p.mist],
      ["--color-line", p.line],
      ["--color-ink", p.ink],
      ["--color-text", p.text],
      ["--color-muted", p.muted],
      ["--color-ph-veg", p.phVeg],
      ["--color-ph-deep", p.phDeep],
      ["--color-scrim", p.scrim],
      ["--shadow-float", p.shadowFloat],
      ["--shadow-lift", p.shadowLift],
      ["--main-opacity-color", p.mainOpacity],
      ["--color-hero-top", p.heroTop],
      ["--color-hero-bottom", p.heroBottom],
      ["--color-card-top", p.cardTop],
      ["--color-card-mid", p.cardMid],
      ["--color-card-bottom", p.cardBottom],
      ["--color-check-bg", p.checkBg],
    ];
    return decl.map((pair) => pair[0] + ":" + pair[1]).join(";");
  }

  function choices() {
    return [
      { id: "PRODUCE", name: "青绿", swatch: PRODUCE.primary },
      { id: "BREAKFAST", name: "赭石", swatch: resolve({ themePreset: "BREAKFAST" }).primary },
      { id: "EATERY", name: "绛红", swatch: resolve({ themePreset: "EATERY" }).primary },
      { id: "MARKET", name: "海蓝", swatch: resolve({ themePreset: "MARKET" }).primary },
      { id: "CUSTOM", name: "自定义", swatch: "" },
    ];
  }

  function formState(store) {
    const preset = readPreset(store && store.themePreset);
    const palette = resolve(store);
    const hueSource = preset === "CUSTOM" && normalizeHex(store && store.themePrimary)
      ? palette.primary
      : resolve({ themePreset: preset === "CUSTOM" ? "PRODUCE" : preset }).primary;
    return {
      themePreset: preset,
      themePrimary: preset === "CUSTOM" ? palette.primary : "",
      themeHue: hueOf(preset === "CUSTOM" ? palette.primary : hueSource),
    };
  }

  return {
    readPreset: readPreset,
    normalizeHex: normalizeHex,
    hueOf: hueOf,
    canonicalPrimary: canonicalPrimary,
    primaryFromHue: primaryFromHue,
    resolve: resolve,
    pageStyle: pageStyle,
    choices: choices,
    formState: formState,
    contrast: contrast,
  };
});
