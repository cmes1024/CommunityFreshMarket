const config = require("./config");
const { getToken } = require("./request");

function uploadImage(filePath) {
  return new Promise((resolve, reject) => {
    const token = getToken();
    wx.uploadFile({
      url: `${config.apiHost}/api/v1/upload/image`,
      filePath,
      name: "file",
      header: {
        Authorization: token ? `Bearer ${token}` : "",
        "X-Store-Id": String(config.defaultStoreId),
      },
      success: (res) => {
        try {
          const body = JSON.parse(res.data || "{}");
          if (body.code === 0 && body.data && body.data.url) {
            resolve(body.data.url);
            return;
          }
          wx.showToast({ title: body.message || "上传失败", icon: "none" });
          reject(body);
        } catch (e) {
          reject(e);
        }
      },
      fail: reject,
    });
  });
}

function chooseAndUpload(count = 1) {
  const n = Math.max(1, Math.min(9, Number(count) || 1));
  return new Promise((resolve, reject) => {
    wx.chooseMedia({
      count: n,
      mediaType: ["image"],
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

module.exports = { uploadImage, chooseAndUpload };
