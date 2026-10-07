const api = require("../../../utils/api");

Page({
  data: { id: 0, form: { phone: "", community: "", building: "", room: "" } },
  async onLoad(query) {
    const id = query.id ? Number(query.id) : 0;
    if (id) {
      const form = await api.address(id);
      this.setData({ id, form });
    }
  },
  onField(e) {
    const k = e.currentTarget.dataset.k;
    this.setData({ [`form.${k}`]: e.detail.value });
  },
  async save() {
    const { phone, community, building, room } = this.data.form;
    if (!phone || !community) {
      wx.showToast({ title: "请填写手机号和小区", icon: "none" });
      return;
    }
    const body = { phone, community, building, room, isDefault: true };
    if (this.data.id) await api.addressUpdate(this.data.id, body);
    else await api.addressCreate(body);
    wx.navigateBack();
  },
});
