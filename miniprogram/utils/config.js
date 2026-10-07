module.exports = {
  version: "0.1.0",
  env: "dev",
  /** 模拟器可用本机；真机改为电脑局域网 IP，如 http://192.168.1.8:8088 */
  apiHost: "http://127.0.0.1:8088",
  defaultStoreId: 1,
  /** 与 deploy/.env 里 WX_SUBSCRIBE_RESTOCK_TEMPLATE_ID 保持一致 */
  restockTemplateId: "XlN1YYf6W4aOi8Da2IiGNGqSIFIHptmf2JnEXBnGtcQ",
};
