# 部署（本地 / 单机）

在本目录执行：

```bash
cp .env.example .env
# 填入 WX_APPID / WX_SECRET；店主 openid 见下方「绑店主」
docker compose up -d --build
```

验证：

```bash
curl -s http://127.0.0.1:8088/api/v1/health
# {"code":0,"message":"ok","data":{"status":"UP","db":"UP"}}
```

## 绑店主（本地与生产相同）

登录必须走真实微信 `code`（需已配置 AppID/Secret）。本机若开了代理，在 `deploy/.env` 设置 `HTTPS_PROXY=http://host.docker.internal:7890`，否则容器直连微信可能握手挂起，登录接口无响应。

1. 用店主微信打开小程序，完成一次登录  
2. 查 openid：

```bash
docker exec -it cfm-postgres psql -U cfm -d cfm \
  -c 'SELECT id, openid, nickname FROM "user" ORDER BY id;'
```

3. 写入 `deploy/.env`：

```bash
BOOTSTRAP_OWNER_OPENID=从上面查到的openid
```

4. 重启 API：`docker compose up -d api`  
5. 店主再进一次小程序（触发登录）→「我的」出现商家工作台  

也可直接刷库：

```sql
INSERT INTO store_member (store_id, user_id, role, created_at, updated_at)
VALUES (1, <user_id>, 'OWNER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
```

## 接口抽查（需小程序登录后的 JWT）

```bash
TOKEN=...
curl -s http://127.0.0.1:8088/api/v1/me -H "Authorization: Bearer $TOKEN"
curl -s http://127.0.0.1:8088/api/v1/categories -H "Authorization: Bearer $TOKEN"
curl -s http://127.0.0.1:8088/api/v1/products -H "Authorization: Bearer $TOKEN"

# 商家新建商品（须已是店铺成员）
curl -s http://127.0.0.1:8088/api/v1/merchant/products \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"categoryId":1,"name":"油麦菜","unit":"JIN","priceFen":450,"inStock":true,"onShelf":true}'
```

履约设置 / 上传 / 到货提醒：

- `PUT /api/v1/merchant/store`（含 `heroImageUrl`）
- `POST /api/v1/merchant/pickup-slots`
- `POST /api/v1/upload/image`（multipart file）
- `POST /api/v1/products/{id}/restock-watch`

环境变量说明见 `.env.example`（含四类订阅消息模板；以及邀请店员 `INVITE_LINK_BASE`）。

**邀请店员 · 公众平台配置（扫普通链接二维码打开小程序）**

| 项 | 值 |
|----|-----|
| 链接前缀 | 与 `INVITE_LINK_BASE` 一致，例 `https://your.example.com/cfm/join` |
| 完整示例 | `https://your.example.com/cfm/join?code=AB12CD` |
| 小程序页面 | `packageCustomer/pages/join-store/join-store` |
| 参数名 | `code`（邀请码） |

生产务必更换 `JWT_SECRET`、数据库密码，并配置真实微信密钥与订阅模板 ID；小程序 `utils/config.js` 须与 `.env` 中模板 ID 一致。
