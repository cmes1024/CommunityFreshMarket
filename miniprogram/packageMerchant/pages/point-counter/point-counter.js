const api = require("../../../utils/api");
const copy = require("../../../utils/copy");
const { format } = require("../../../utils/copy");
const { absUrl } = require("../../../utils/upload");

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
    fromText: copy.merchantCounter.fromBubble,
  };
}

Page({
  data: {
    bubbles: [],
    code: "",
    checkinId: "",
    memberLabel: "",
    selected: null,
    amountYuan: "",
    previewText: "",
    pointsPerYuan: 1,
    submitting: false,
  },
  onShow() {
    this.loadStore();
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
  },
  async loadStore() {
    try {
      const store = await api.store();
      const pointsPerYuan = store && store.pointsPerYuan != null ? store.pointsPerYuan : 1;
      this.setData({ pointsPerYuan });
      this.updatePreview();
    } catch (e) {}
  },
  async loadBubbles() {
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
      const selectedId = Number(this.data.checkinId);
      const current = selectedId
        ? list.find((item) => Number(item.id) === selectedId)
        : null;
      const patch = { bubbles: list };
      if (selectedId && !current) {
        patch.checkinId = "";
        patch.memberLabel = "";
        patch.selected = null;
      } else if (current) {
        patch.memberLabel = current.displayName;
        patch.selected = bubbleSelected(current);
      }
      this.setData(patch);
    } catch (e) {}
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
      wx.showToast({ title: copy.merchantCounter.notMemberCode, icon: "none" });
      return;
    }
    this.setData({
      code,
      checkinId: "",
      memberLabel: copy.merchantCounter.scanned,
      selected: {
        name: copy.merchantCounter.scanned,
        avatarSrc: "",
        initial: "",
        fromText: "",
      },
    });
  },
  pickBubble(e) {
    const id = Number(e.currentTarget.dataset.id);
    const item = (this.data.bubbles || []).find((row) => Number(row.id) === id);
    if (!item) return;
    this.setData({
      checkinId: id,
      code: "",
      memberLabel: item.displayName,
      selected: bubbleSelected(item),
    });
  },
  onAmount(e) {
    this.setData({ amountYuan: e.detail.value });
    this.updatePreview();
  },
  updatePreview() {
    const yuan = Number(this.data.amountYuan);
    const ratio = this.data.pointsPerYuan || 0;
    if (!ratio || !Number.isFinite(yuan) || yuan <= 0) {
      this.setData({ previewText: "" });
      return;
    }
    const points = Math.floor(Math.round(yuan * 100) / 100) * ratio;
    this.setData({
      previewText: points > 0
        ? format(copy.merchantCounter.preview, { n: points })
        : copy.merchantCounter.tooSmall,
    });
  },
  async submit() {
    if (this.data.submitting) return;
    if (!this.data.pointsPerYuan) {
      wx.showToast({ title: copy.merchantCounter.pointsOff, icon: "none" });
      return;
    }
    if (!this.data.code && !this.data.checkinId) {
      wx.showToast({ title: copy.merchantCounter.needMember, icon: "none" });
      return;
    }
    const yuan = Number(this.data.amountYuan);
    const amountFen = Math.round(yuan * 100);
    if (!Number.isFinite(yuan) || amountFen <= 0) {
      wx.showToast({ title: copy.merchantCounter.needAmount, icon: "none" });
      return;
    }
    if (Math.floor(amountFen / 100) * this.data.pointsPerYuan <= 0) {
      wx.showToast({ title: copy.merchantCounter.tooSmall, icon: "none" });
      return;
    }
    const body = { amountFen };
    if (this.data.checkinId) body.checkinId = this.data.checkinId;
    else body.code = this.data.code;
    this.setData({ submitting: true });
    try {
      const res = await api.merchantCounterPoints(body);
      const name = (res && res.nickname) || this.data.memberLabel || "会员";
      wx.showToast({
        title: format(copy.merchantCounter.done, { name, n: res.points }),
        icon: "none",
      });
      this.setData({
        code: "",
        checkinId: "",
        memberLabel: "",
        selected: null,
        amountYuan: "",
        previewText: "",
      });
      await this.loadBubbles();
    } catch (e) {
    } finally {
      this.setData({ submitting: false });
    }
  },
});
