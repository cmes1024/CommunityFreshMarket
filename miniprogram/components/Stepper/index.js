Component({
  properties: {
    value: { type: Number, value: 0 },
    min: { type: Number, value: 0 },
    max: { type: Number, value: 999 },
    step: { type: Number, value: 1 },
    disabled: { type: Boolean, value: false },
    /** md 默认；sm 用于三列卡等紧凑场景 */
    size: { type: String, value: "md" },
  },
  observers: {
    value(v) {
      const n = Number(v) || 0;
      this.setData({ display: Number.isInteger(n) ? String(n) : n.toFixed(2) });
    },
  },
  data: { display: "0" },
  methods: {
    onMinus() {
      if (this.data.disabled) return;
      const next = Math.max(this.data.min, Number((this.data.value - this.data.step).toFixed(2)));
      if (next === this.data.value) return;
      this.triggerEvent("change", { value: next });
    },
    onPlus() {
      if (this.data.disabled) return;
      const next = Math.min(this.data.max, Number((this.data.value + this.data.step).toFixed(2)));
      if (next === this.data.value) return;
      this.triggerEvent("change", { value: next });
    },
  },
});
