const api = require("../../../utils/api");
const copy = require("../../../utils/copy");

function toQrSrc(res) {
  const b64 = res && res.qrcodeBase64;
  if (!b64) return "";
  return `data:image/png;base64,${b64}`;
}

Page({
  data: {
    qrSrc: "",
    nickname: "",
  },
  async onShow() {
    try {
      const res = await api.memberCode();
      this.setData({
        qrSrc: toQrSrc(res),
        nickname: (res && res.nickname) || "",
      });
    } catch (e) {
      this.setData({ qrSrc: "" });
    }
  },
  async bubble() {
    try {
      await api.checkin({});
      wx.showToast({ title: copy.memberCode.bubbled, icon: "none" });
    } catch (e) {}
  },
});
