function fenToYuan(fen) {
  const n = Number(fen) || 0;
  return (n / 100).toFixed(2);
}

function yuanPlain(fen) {
  const n = Math.round(Number(fen) || 0) / 100;
  return String(Math.round(n * 100) / 100);
}

function yuanText(fen) {
  return `¥${fenToYuan(fen)}`;
}

module.exports = { fenToYuan, yuanPlain, yuanText };
