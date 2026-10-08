function sanitizeRatioInput(raw) {
  let value = String(raw ?? "").replace(/[^\d.]/g, "");
  const dot = value.indexOf(".");
  if (dot >= 0) {
    value = value.slice(0, dot + 1) + value.slice(dot + 1).replace(/\./g, "").slice(0, 2);
  }
  if (value.startsWith(".")) value = `0${value}`;
  return value;
}

function formatRatio(n) {
  const num = Number(n);
  if (!Number.isFinite(num)) return "0";
  return String(parseFloat(num.toFixed(2)));
}

function parseRatio(raw) {
  const text = String(raw ?? "").trim();
  if (!/^\d+(\.\d{1,2})?$/.test(text)) return null;
  const n = Number(text);
  if (!Number.isFinite(n) || n < 0 || n > 100) return null;
  return Number(formatRatio(n));
}

function earnPoints(amountFen, ratio) {
  const hundredths = Math.round(Number(ratio) * 100);
  if (!Number.isFinite(hundredths) || hundredths <= 0 || amountFen < 100) return 0;
  return Math.floor((Math.floor(amountFen / 100) * hundredths) / 100);
}

module.exports = { sanitizeRatioInput, formatRatio, parseRatio, earnPoints };
