const { getStorage, setStorage } = require("./storage");
const config = require("./config");

const TOKEN_KEY = "cfm_token";
const USER_KEY = "cfm_user";

class RequestManager {
  #requestStatus = null;
  #requestTask = new Set();
  #requestWhiteList = ["/api/v1/auth/wechat", "/api/v1/health", "/api/v1/meta"];
  #requstTime = 20 * 1000;
  #requstStatusMap = new Map([
    ["REQUESTLOADING", 1],
    ["HASREQUEST", 2],
  ]);
  #idleCallback = (() => {
    if (typeof requestIdleCallback === "function") {
      return (fn, options) => requestIdleCallback(fn, options);
    }
    return (fn) => setTimeout(() => fn({ didTimeout: true }), 300);
  })();

  constructor() {}

  setWhiteList(list) {
    this.#requestWhiteList = list;
  }

  async preTreatment(options = {}, callback) {
    if (typeof options === "function" && !callback) callback = options;
    if (options.idle && typeof callback === "function") {
      this.#idleCallback(() => callback(), { timeout: options.idleTimeout || 3000 });
      return;
    }
    if (options.startRequest) {
      this.#requestStatus = this.#requstStatusMap.get("REQUESTLOADING");
      this.#requestTask.add(callback);
      this.#requestStatus = this.#requstStatusMap.get("HASREQUEST");
      this.#requestTask.forEach((resolve) => {
        if (typeof resolve === "function") resolve();
      });
      this.#requestTask.clear();
    } else if (options.clear) {
      this.#requestTask.clear();
    } else if (
      !this.#requestWhiteList.length ||
      this.#requestStatus === this.#requstStatusMap.get("HASREQUEST") ||
      this.#requestWhiteList.map((item) => (options?.url || "").indexOf(item) >= 0).includes(true)
    ) {
      if (typeof callback === "function") callback();
    } else if (this.#requestStatus === this.#requstStatusMap.get("REQUESTLOADING")) {
      typeof callback == "function" && this.#requestTask.add(callback);
    } else {
      this.#requestTask.add(callback);
    }
  }

  request(method, path, data, options = {}) {
    const url = path.startsWith("http") ? path : `${config.apiHost}${path}`;
    return new Promise((resolve, reject) => {
      this.preTreatment({ url }, () => {
        this.#send({ method, url, data, options, resolve, reject, retried: false });
      });
    });
  }

  #send({ method, url, data, options, resolve, reject, retried }) {
    const header = {
      "content-type": "application/json",
      "X-Store-Id": String(config.defaultStoreId),
      ...(options.headers || {}),
    };
    if (!options.skipAuth) {
      const token = getStorage(TOKEN_KEY);
      if (token) header.Authorization = `Bearer ${token}`;
    }
    this.basicRequest({
      url,
      data: data || {},
      method: method || "GET",
      timeout: options.timeout || this.#requstTime,
      header,
      success: async (response) => {
        const body = response.data || {};
        const code = body.code;
        if (code === 40100 || code === 40101) {
          if (!retried && !options.skipAuth) {
            try {
              await require("./auth").login(true);
              this.#send({ method, url, data, options, resolve, reject, retried: true });
              return;
            } catch (e) {
              reject(body);
              return;
            }
          }
          reject(body);
          return;
        }
        if (code === 0) {
          resolve(body.data);
          return;
        }
        const message = body.message || "请求失败";
        if (!options.hideError) {
          wx.showToast({ title: message, icon: "none" });
        }
        reject(body);
      },
      fail: (err) => {
        if (!options.hideError) {
          wx.showToast({ title: "网络开小差啦～", icon: "none" });
        }
        reject(err);
      },
    });
  }

  basicRequest(requestDatas) {
    const { url, data, method, timeout, header, success, fail, complete } = requestDatas;
    return wx.request({
      url,
      data,
      timeout: timeout || this.#requstTime,
      header,
      method,
      success,
      fail,
      complete,
    });
  }
}

function getClient() {
  const app = getApp();
  if (!app.globalData.request) {
    app.globalData.request = new RequestManager();
  }
  return app.globalData.request;
}

function http(method, path, data, options) {
  return getClient().request(method, path, data, options);
}

module.exports = {
  RequestManager,
  TOKEN_KEY,
  USER_KEY,
  http,
  get: (path, data, options) => http("GET", path, data, options),
  post: (path, data, options) => http("POST", path, data, options),
  put: (path, data, options) => http("PUT", path, data, options),
  del: (path, data, options) => http("DELETE", path, data, options),
  getToken: () => getStorage(TOKEN_KEY),
  setToken: (token) => setStorage(TOKEN_KEY, token),
  getUser: () => getStorage(USER_KEY),
  setUser: (user) => setStorage(USER_KEY, user),
};
