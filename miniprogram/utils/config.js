module.exports = {
  version: "0.1.0",
  env: "dev",
  /** 模拟器可用本机；真机改为电脑局域网 IP，如 http://192.168.1.8:8088 */
  apiHost: "http://127.0.0.1:8088",
  defaultStoreId: 1,

  /**
   * 微信订阅消息模板 ID（与 deploy/.env 的 WX_SUBSCRIBE_* 一致）。
   * 空字符串 = 跳过 requestSubscribeMessage，服务端也不会真正下发（只打日志）。
   * 一次授权最多 3 个 tmplId。到货提醒字段为 thing1 商品名、thing2 门店名、thing3 提示；
   * 其余模板为 thing1 摘要、thing2 提示。见服务端 SubscribeMessageService。
   */

  /** 到货提醒 ← WX_SUBSCRIBE_RESTOCK_TEMPLATE_ID；授权：顾客售罄「到货提醒我」；触发：改回有货 */
  restockTemplateId: "jsmc4ocWFhLBTegZOxUCqohdt4fNJUX2OHRh0ElqQeI",

  /** 新预约单 ← WX_SUBSCRIBE_NEW_ORDER_TEMPLATE_ID；授权：店员点「开启订单通知」；触发：下单 → 仅已授权店员 */
  newOrderTemplateId: "nt_pzDeFntuTlRbcueAb3qOfTzPGQc-FeFeRFAA3SSA",

  /** 订单取消 ← WX_SUBSCRIBE_CANCEL_TEMPLATE_ID；授权：店员「开启订单通知」；触发：顾客取消 → 已授权店员 */
  cancelTemplateId: "fZA3NyTa2nDLup5ShCHfJ78E_wfRgUchnfvMO8g6xWk",

  /** 配送出发 ← WX_SUBSCRIBE_DELIVERING_TEMPLATE_ID；授权：顾客提交上门配送；触发：推进到配送中 → 该顾客 */
  deliveringTemplateId: "OUezfKtgTy9JRJlX5s_6_Aa_Ax1nouD2qL5_qhuuciw",
};
