const assert = require("node:assert/strict");
const test = require("node:test");
const theme = require("./theme");

test("produce stays the hand-tuned brand green", () => {
  const palette = theme.resolve(null);
  assert.equal(palette.id, "PRODUCE");
  assert.equal(palette.primary, "#0f766e");
  assert.equal(palette.text, "#134e4a");
  assert.equal(palette.background, "#f0fdfa");
});

test("generated themes keep white button text and body text readable", () => {
  const samples = []
    .concat(["BREAKFAST", "EATERY", "MARKET"].map((id) => theme.resolve({ themePreset: id })))
    .concat([0, 20, 45, 60, 90, 130, 174, 200, 230, 270, 310, 345].map((hue) => {
      return theme.resolve({ themePreset: "CUSTOM", themePrimary: theme.primaryFromHue(hue) });
    }));
  samples.forEach((palette) => {
    assert.ok(theme.contrast(palette.primary, "#ffffff") >= 4.5, palette.primary + " on white");
    assert.ok(theme.contrast(palette.text, "#ffffff") >= 4.5, palette.text + " text");
    assert.ok(theme.contrast(palette.text, palette.background) >= 4.5, palette.id + " text on bg");
    assert.ok(theme.contrast(palette.muted, "#ffffff") >= 4.5, palette.id + " muted");
    assert.ok(theme.contrast(palette.muted, palette.background) >= 4.5, palette.id + " muted on bg");
  });
});

test("presets stay in the deeper band and custom blue can go brighter", () => {
  assert.equal(theme.resolve({ themePreset: "BREAKFAST" }).primary, "#994724");
  assert.equal(theme.resolve({ themePreset: "EATERY" }).primary, "#a5272f");
  assert.equal(theme.resolve({ themePreset: "MARKET" }).primary, "#25619d");
  const blue = theme.primaryFromHue(240);
  assert.ok(theme.contrast(blue, "#ffffff") >= 4.6, blue);
  assert.notEqual(blue, "#2727a5");
});

test("canonical primary is stable and custom without a color falls back to teal", () => {
  const once = theme.canonicalPrimary("#fde68a");
  assert.equal(theme.canonicalPrimary(once), once);
  assert.equal(theme.resolve({ themePreset: "CUSTOM" }).primary, "#0f766e");
  assert.equal(theme.resolve({ themePreset: "TEAL" }).id, "PRODUCE");
  assert.equal(theme.resolve({ themePreset: "nope" }).id, "PRODUCE");
});

test("page style overrides brand tokens and leaves price alone", () => {
  const css = theme.pageStyle(theme.resolve({ themePreset: "MARKET" }));
  assert.match(css, /--color-primary:/);
  assert.equal(css.includes("--color-price"), false);
  assert.equal(css.includes("--color-danger"), false);
});
