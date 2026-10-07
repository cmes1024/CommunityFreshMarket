# Community Fresh Market

面向中小商户的**私域经营工具**：微信小程序（顾客端 + 商家端）+ Java 后端。

不做在线支付，聚焦商品介绍、预约履约（配送 / 自提）与后续会员经营；当面收款在线下完成。

## 当前阶段

产品、技术方案与第一波 UI 原型已对齐；下一步可按技设做工程脚手架（Docker + Spring Boot + 自模板裁剪小程序）。

## 文档

- 产品设计：[docs/plans/2026-10-07-product-design.md](docs/plans/2026-10-07-product-design.md)
- 技术方案：[docs/plans/2026-10-07-technical-design.md](docs/plans/2026-10-07-technical-design.md)
- UI 原型说明：[docs/plans/2026-10-07-ui-prototype.md](docs/plans/2026-10-07-ui-prototype.md)
- 可点击原型：[docs/prototypes/index.html](docs/prototypes/index.html)（浏览器直接打开）
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
2. **第二波**：积分、图像选品、简单库存
3. **第三波**：优惠券、到店收银、会员快速定位

## 给协作者 / Agent

仓库约定见 [AGENTS.md](AGENTS.md)。
