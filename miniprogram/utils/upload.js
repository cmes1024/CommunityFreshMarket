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

function chooseAndUpload() {
  return new Promise((resolve, reject) => {
    wx.chooseMedia({
      count: 1,
      mediaType: ["image"],
      success: async (res) => {
        try {
          const path = res.tempFiles[0].tempFilePath;
          resolve(await uploadImage(path));
        } catch (e) {
          reject(e);
        }
      },
      fail: reject,
    });
  });
}

module.exports = { uploadImage, chooseAndUpload };
