const api = require("../../../utils/api");
const copy = require("../../../utils/copy");
const { format } = require("../../../utils/copy");
const { absUrl } = require("../../../utils/upload");
const { fenToYuan } = require("../../../utils/money");

const MEMBER_PREFIX = "CFM-MEMBER:";

function bubbleName(item) {
  const nick = item && item.nickname && String(item.nickname).trim();
  return nick || copy.profile.defaultNickname;
}

function bubbleSelected(item) {
  return {
    name: item.displayName,
    avatarSrc: item.avatarSrc || "",
    initial: item.initial,
    fromText: copy.merchantBalance.fromBubble,
  };
}

/**
 * 冒泡列表每 5 秒拉一次。列表挂在弹层里，弹层没开时 setData 只是数据，
 * 开着时每次都会把整排头像重新渲染一遍 —— 真机上就是一直卡。
 * 内容没变就别推，签名只取影响渲染的字段。
 */
function bubbleSig(list) {
  return (list || [])
    .map((item) => `${item.id}:${item.avatarSrc || ""}:${item.displayName}`)
    .join("|");
}

Page({
  data: {
    bubbles: [],
    code: "",
    checkinId: "",
    selected: null,
    amountYuan: "",
    submitting: false,
    bubbleSheet: false,
  },
  onShow() {
    this.startWatch();
  },
  onHide() {
    this.stopWatch();
  },
  onUnload() {
    this.stopWatch();
  },
  startWatch() {
    this.stopWatch();
    this.loadBubbles();
    this._timer = setInterval(() => this.loadBubbles(), 5000);
  },
  stopWatch() {
    if (this._timer) {
      clearInterval(this._timer);
      this._timer = null;
    }
    if (this._openTimer) {
      clearTimeout(this._openTimer);
      this._openTimer = null;
    }
  },
  async loadBubbles() {
    if (this._loadingBubbles) return;
    this._loadingBubbles = true;
    try {
      const bubbles = await api.merchantCheckinsActive();
      const list = (bubbles || []).map((item) => {
        const displayName = bubbleName(item);
        return {
          ...item,
          displayName,
          avatarSrc: absUrl(item.avatarUrl),
          initial: displayName.slice(0, 1),
        };
      });
      const patch = {};
      const sig = bubbleSig(list);
      if (sig !== this._bubbleSig) {
        this._bubbleSig = sig;
        patch.bubbles = list;
      }
      const selectedId = Number(this.data.checkinId);
      const current = selectedId ? list.find((item) => Number(item.id) === selectedId) : null;
      if (selectedId && !current) {
        patch.checkinId = "";
        patch.selected = null;
      } else if (current && (!this.data.selected || this.data.selected.name !== current.displayName)) {
        patch.selected = bubbleSelected(current);
      }
      if (!Object.keys(patch).length) return;
      this.setData(patch);
    } catch (e) {
    } finally {
      this._loadingBubbles = false;
    }
  },
  async scan() {
    let code = "";
    try {
      const res = await new Promise((resolve, reject) => {
        wx.scanCode({ success: resolve, fail: reject });
      });
      code = String(res.result || "").trim();
    } catch (e) {
      return;
    }
    if (!code.startsWith(MEMBER_PREFIX)) {
      wx.showToast({ title: copy.merchantBalance.notMemberCode, icon: "none" });
      return;
    }
    this.setData({
      code,
      checkinId: "",
      selected: {
        name: copy.merchantBalance.scanned,
        avatarSrc: "",
        initial: "",
        fromText: "",
      },
    });
  },
  openBubbles() {
    if (this.data.bubbleSheet) return;
    this.setData({ bubbleSheet: true });
    // 滑入动画只有 260ms，这时候再推整表会和动画抢渲染，面板明显掉帧
    if (this._openTimer) clearTimeout(this._openTimer);
    this._openTimer = setTimeout(() => {
      this._openTimer = null;
      this.loadBubbles();
    }, 320);
  },
  closeBubbles() {
    if (!this.data.bubbleSheet) return;
    this.setData({ bubbleSheet: false });
  },
  noop() {},
  pickBubble(e) {
    const id = Number(e.currentTarget.dataset.id);
    const item = (this.data.bubbles || []).find((row) => Number(row.id) === id);
    if (!item) return;
    this.setData({
      checkinId: id,
      code: "",
      selected: bubbleSelected(item),
      bubbleSheet: false,
    });
  },
  onAmount(e) {
    this.setData({ amountYuan: e.detail.value });
  },
  async submit() {
    if (this.data.submitting) return;
    if (!this.data.code && !this.data.checkinId) {
      wx.showToast({ title: copy.merchantBalance.needMember, icon: "none" });
      return;
    }
    const yuan = Number(this.data.amountYuan);
    const amountFen = Math.round(yuan * 100);
    if (!Number.isFinite(yuan) || amountFen <= 0) {
      wx.showToast({ title: copy.merchantBalance.needAmount, icon: "none" });
      return;
    }
    const body = { amountFen };
    if (this.data.checkinId) body.checkinId = this.data.checkinId;
    else body.code = this.data.code;
    this.setData({ submitting: true });
    try {
      const res = await api.merchantBalanceTopup(body);
      const name = (res && res.nickname) || (this.data.selected && this.data.selected.name) || "会员";
      wx.showToast({
        title: format(copy.merchantBalance.done, { name, yuan: fenToYuan(res.amountFen || amountFen) }),
        icon: "none",
      });
      this.setData({
        code: "",
        checkinId: "",
        selected: null,
        amountYuan: "",
      });
      await this.loadBubbles();
    } catch (e) {
    } finally {
      this.setData({ submitting: false });
    }
  },
});
