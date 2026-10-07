function fenToYuan(fen) {
  const n = Number(fen) || 0;
  return (n / 100).toFixed(2);
}

function yuanText(fen) {
  return `¥${fenToYuan(fen)}`;
}

module.exports = { fenToYuan, yuanText };
