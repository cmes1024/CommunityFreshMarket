/**
 * 半屏弹层，替代 page-container。
 *
 * 为什么不用 page-container：它的原生容器在所有页面上都会创建一层，真机
 * （尤其 iOS）上会拖慢页面切换，表现为点入口白屏、之后整机越来越卡。
 * 这里用普通节点 + transform 做，开销可预期。
 *
 * 用法：<BottomSheet show="{{open}}" bind:close="onClose">内容</BottomSheet>
 * 页面只维护 show，挂载与卸载、进出动画都由组件内部处理。
 */
Component({
  properties: {
    show: {
      type: Boolean,
      value: false,
      observer(v) {
        this.toggle(v);
      },
    },
    /** 遮罩层级，面板自动 +1 */
    zIndex: { type: Number, value: 900 },
    /** 面板的额外内联样式（高度等） */
    panelStyle: { type: String, value: "" },
    /** 点击遮罩是否关闭 */
    maskClosable: { type: Boolean, value: true },
  },
  data: {
    mounted: false,
    open: false,
  },
  lifetimes: {
    attached() {
      // 首次挂载时就要求展开（例如页面 onLoad 直接置 true）
      if (this.data.show) this.toggle(true);
    },
    detached() {
      this.clearTimer();
    },
  },
  methods: {
    clearTimer() {
      if (this._timer) {
        clearTimeout(this._timer);
        this._timer = null;
      }
    },
    toggle(show) {
      this.clearTimer();
      if (show) {
        this.setData({ mounted: true });
        // 先挂载再拉起，滑入动画才不会被吞掉
        wx.nextTick(() => this.setData({ open: true }));
        return;
      }
      if (!this.data.mounted) return;
      this.setData({ open: false });
      this._timer = setTimeout(() => {
        this._timer = null;
        this.setData({ mounted: false });
      }, 260);
    },
    onMaskTap() {
      if (this.data.maskClosable) this.triggerEvent("close");
    },
    noop() {},
  },
});
