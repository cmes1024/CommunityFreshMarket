const config = require("./config");
const { getToken } = require("./request");

const MAX_EDGE = 1080;
const JPEG_QUALITY = 50;

function compress(filePath) {
  return new Promise((resolve) => {
    const finish = (res) => resolve((res && res.tempFilePath) || filePath);
    const run = (extra) => {
      wx.compressImage({
        src: filePath,
        quality: JPEG_QUALITY,
        ...extra,
        success: finish,
        fail: () => {
          if (extra && Object.keys(extra).length) {
            run({});
            return;
          }
          resolve(filePath);
        },
      });
    };
    wx.getImageInfo({
      src: filePath,
      success: (info) => {
        const w = info.width || 0;
        const h = info.height || 0;
        const extra = {};
        if (w >= h && w > MAX_EDGE) extra.compressedWidth = MAX_EDGE;
        else if (h > w && h > MAX_EDGE) extra.compressedHeight = MAX_EDGE;
        run(extra);
      },
      fail: () => run({ compressedWidth: MAX_EDGE }),
    });
  });
}

function uploadImage(filePath) {
  return compress(filePath).then(
    (path) =>
      new Promise((resolve, reject) => {
        const token = getToken();
        wx.uploadFile({
          url: `${config.apiHost}/api/v1/upload/image`,
          filePath: path,
          name: "file",
          header: {
            Authorization: token ? `Bearer ${token}` : "",
            "X-Store-Id": String(config.defaultStoreId),
          },
          success: (res) => {
            if (res.statusCode === 413) {
              wx.showToast({ title: "图片太大，请换一张较小的", icon: "none" });
              reject(res);
              return;
            }
            try {
              const body = JSON.parse(res.data || "{}");
              if (body.code === 0 && body.data && body.data.url) {
                resolve(body.data.url);
                return;
              }
              wx.showToast({ title: body.message || "上传失败", icon: "none" });
              reject(body);
            } catch (e) {
              wx.showToast({ title: "上传失败", icon: "none" });
              reject(e);
            }
          },
          fail: (err) => {
            wx.showToast({ title: "上传失败", icon: "none" });
            reject(err);
          },
        });
      })
  );
}

function chooseAndUpload(count = 1) {
  const n = Math.max(1, Math.min(9, Number(count) || 1));
  return new Promise((resolve, reject) => {
    wx.chooseMedia({
      count: n,
      mediaType: ["image"],
      sizeType: ["compressed"],
      success: async (res) => {
        try {
          const files = res.tempFiles || [];
          if (n === 1) {
            resolve(await uploadImage(files[0].tempFilePath));
            return;
          }
          const urls = [];
          for (const f of files) {
            urls.push(await uploadImage(f.tempFilePath));
          }
          resolve(urls);
        } catch (e) {
          reject(e);
        }
      },
      fail: reject,
    });
  });
}

function chooseLivePhoto() {
  return new Promise((resolve, reject) => {
    wx.chooseMedia({
      count: 1,
      mediaType: ["image"],
      sourceType: ["album", "camera"],
      sizeType: ["compressed"],
      success: async (res) => {
        try {
          const file = (res.tempFiles || [])[0];
          if (!file) {
            reject(new Error("empty"));
            return;
          }
          resolve(await uploadImage(file.tempFilePath));
        } catch (e) {
          reject(e);
        }
      },
      fail: reject,
    });
  });
}

function absUrl(url) {
  if (!url) return "";
  const s = String(url);
  if (/^(https?:|wxfile:|data:|cloud:)/.test(s)) return s;
  return `${config.apiHost}${s.startsWith("/") ? s : `/${s}`}`;
}

module.exports = { uploadImage, chooseAndUpload, chooseLivePhoto, absUrl };
