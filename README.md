# Community Fresh Market

面向中小商户的**私域经营工具**：微信小程序（顾客端 + 商家端）+ Java 后端。

不做在线支付，聚焦商品介绍、预约履约（配送 / 自提）与后续会员经营；当面收款在线下完成。

## 当前阶段

产品 / 技术方案 / UI 原型已对齐。工程脚手架已落地：`server/`（Spring Boot）+ `deploy/`（Compose）。

本地起后端：

```bash
cd deploy && cp .env.example .env && docker compose up -d --build
curl -s http://127.0.0.1:8088/api/v1/health
```

微信开发者工具打开 `miniprogram/`（见该目录 README）。已打通：登录、逛店加购、地址、提交预约、商家接单推进。

第一波后端主路径与小程序主界面已齐；订阅消息需配置 `WX_SUBSCRIBE_RESTOCK_TEMPLATE_ID` 后才会真推。

## 文档

- 产品设计：[docs/plans/2026-10-07-product-design.md](docs/plans/2026-10-07-product-design.md)
- 技术方案：[docs/plans/2026-10-07-technical-design.md](docs/plans/2026-10-07-technical-design.md)
- UI 原型说明：[docs/plans/2026-10-07-ui-prototype.md](docs/plans/2026-10-07-ui-prototype.md)
- 设计系统：[design-system/communityfreshmarket/MASTER.md](design-system/communityfreshmarket/MASTER.md)
- 可点击 UI 稿：[docs/prototypes/index.html](docs/prototypes/index.html)（浏览器直接打开）
- 原始想法碎片：`mini.docx`

## 技术方向

| 层 | 选型 |
|----|------|
| 客户端 | 原生微信小程序；顾客 / 商家分包（底座：自有空白模板，去云开发） |
| 服务端 | Spring Boot 3 + Java 17 + MyBatis-Plus（非云函数） |
| 数据 | MySQL 8；按 `storeId` 隔离 |
| 部署 | Docker Compose：API + MySQL + Nginx |

## 分期概要

1. **第一波**：商品、预约下单、商家接单履约、地址簿、微信客服、有货/售罄、订阅消息；再来一单、到货提醒、自提「我到店了」、备注短语
2. **第二波**：积分、图像选品、简单库存、商品起购/限购
3. **第三波**：优惠券；到店现场消费由店员扫会员码或点「冒个泡」，按金额补记积分

## 给协作者 / Agent

仓库约定见 [AGENTS.md](AGENTS.md)。
