# 小程序（第一波）

微信开发者工具打开本目录。后端需先起：`deploy/` 下 `docker compose up -d`。

## 联调

1. 详情 → 本地设置：勾选 **不校验合法域名**（`project.config.json` 已 `urlCheck: false`）
2. `utils/config.js`：`apiHost` 模拟器可用 `http://127.0.0.1:8088`；真机改成电脑局域网 IP
3. 登录一律 `wx.login` → 后端换 openid（需配置 `WX_APPID` / `WX_SECRET`）
4. 店主：在 `deploy/.env` 配置 `BOOTSTRAP_OWNER_OPENID`（见 `deploy/README.md`），登录后「我的」出现商家工作台

## 结构

- 主包：启动页 + Header / TabBar / 公共 utils、`extend/`
- `packageCustomer`：首页、分类、商品篮、结算、地址、订单、我的
- `packageMerchant`：订单台、商品/分类、门店设置

无云开发；请求走 `Authorization: Bearer`。

商家设置页可改店铺联系方式、首页头图与自提时段；商品编辑可上传图片；售罄详情可点「到货提醒我」。
