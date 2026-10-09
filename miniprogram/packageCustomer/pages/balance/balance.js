const api = require("../../../utils/api");
const copy = require("../../../utils/copy");
const { fenToYuan } = require("../../../utils/money");

function reasonText(reason) {
  const map = {
    TOPUP: copy.balance.reasonTopup,
    SPEND: copy.balance.reasonSpend,
    REFUND: copy.balance.reasonRefund,
  };
  return map[reason] || reason || "";
}

function fmtTime(iso) {
  if (!iso) return "";
  const s = String(iso).replace("T", " ");
  return s.length > 16 ? s.slice(0, 16) : s;
}

function yuanDelta(fen) {
  const yuan = fenToYuan(Math.abs(fen || 0));
  return (fen || 0) >= 0 ? `+¥${yuan}` : `-¥${yuan}`;
}

Page({
  data: {
    balanceYuan: "0.00",
    recent: [],
  },
  async onShow() {
    try {
      const me = await api.balanceMe();
      this.setData({
        balanceYuan: fenToYuan(me.balanceFen || 0),
        recent: (me.recent || []).map((row) => ({
          ...row,
          timeText: fmtTime(row.createdAt),
          deltaText: yuanDelta(row.deltaFen),
          reasonText: reasonText(row.reason),
          plus: (row.deltaFen || 0) >= 0,
        })),
      });
    } catch (e) {}
  },
});
