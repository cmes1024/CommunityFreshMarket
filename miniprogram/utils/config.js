module.exports = {
  version: "0.1.0",
  env: "dev",
  /** 模拟器可用本机；真机改为电脑局域网 IP，如 http://192.168.1.8:8088 */
  apiHost: "http://127.0.0.1:8088",
  defaultStoreId: 1,

  /**
   * 微信订阅消息模板 ID（与 deploy/.env 的 WX_SUBSCRIBE_* 一致）。
   * 空字符串 = 跳过 requestSubscribeMessage，服务端也不会真正下发（只打日志）。
   * 一次授权最多 3 个 tmplId；字段约定见服务端 SubscribeMessageService（thing1 / thing2）。
   */

  /** 到货提醒 ← WX_SUBSCRIBE_RESTOCK_TEMPLATE_ID；授权：顾客售罄「到货提醒我」；触发：改回有货 */
  restockTemplateId: "XlN1YYf6W4aOi8Da2IiGNGqSIFIHptmf2JnEXBnGtcQ",

  /** 新预约单 ← WX_SUBSCRIBE_NEW_ORDER_TEMPLATE_ID；授权：店员点「开启订单通知」；触发：下单 → 仅已授权店员 */
  newOrderTemplateId: "",

  /** 订单取消 ← WX_SUBSCRIBE_CANCEL_TEMPLATE_ID；授权：顾客点「提交预约」等；触发：取消 → 对端且须已授权 */
  cancelTemplateId: "",

  /** 顾客到店 ← WX_SUBSCRIBE_ARRIVED_TEMPLATE_ID；授权：报到/「开启订单通知」；触发：报到 → 已授权店员 */
  arrivedTemplateId: "",
};
