const config = require("./config");

/**
 * keys 为 config.js 中的模板字段名，例如：
 * - restockTemplateId
 * - newOrderTemplateId
 * - cancelTemplateId
 * - arrivedTemplateId
 */
function collectIds(keys) {
  const ids = [];
  (keys || []).forEach((key) => {
    const id = config[key];
    if (id && !ids.includes(id)) ids.push(id);
  });
  return ids.slice(0, 3);
}

/** 请求订阅授权；无模板 ID 或用户拒绝不阻断业务（一次最多 3 个） */
function requestSubscribe(keys) {
  const tmplIds = collectIds(keys);
  if (!tmplIds.length) return Promise.resolve();
  return new Promise((resolve) => {
    try {
      wx.requestSubscribeMessage({
        tmplIds,
        complete: resolve,
        fail: resolve,
      });
    } catch (e) {
      resolve();
    }
  });
}

module.exports = { requestSubscribe };
