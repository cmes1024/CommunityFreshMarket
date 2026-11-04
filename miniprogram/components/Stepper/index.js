Component({
  // 列表里实例数量多，不需要 windowInfo / deviceInfo 这类全局信息，
  // 关掉全局注入，省掉每个实例的初始 data 与传输
  skipSystemData: true,
  properties: {
    value: { type: Number, value: 0 },
    min: { type: Number, value: 0 },
    max: { type: Number, value: 999 },
    step: { type: Number, value: 1 },
    disabled: { type: Boolean, value: false },
    /** md 默认；sm 用于三列卡等紧凑场景 */
    size: { type: String, value: "md" },
  },
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
