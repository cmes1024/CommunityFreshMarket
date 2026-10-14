# 社区生鲜私域工具 — 技术方案（第一波）

日期：2026-10-07  
状态：已对齐，可据此脚手架  
依据：`docs/plans/2026-10-07-product-design.md`  
UI 原型：`docs/plans/2026-10-07-ui-prototype.md` · [`docs/prototypes/index.html`](../prototypes/index.html)  
小程序底座参考：`/Users/cme/Documents/github/miniprogram`（忽略云开发）

---

## 1. 总体架构与部署

### 1.1 形态

```text
微信小程序 ──HTTPS──▶ Nginx ──/api──▶ Spring Boot API ──▶ MySQL 8
                                              └──上传──▶ MinIO（商品图，公开读）
```

单机 Docker Compose 即可跑通；业务不走微信云函数，避免调用次数限制。

### 1.2 技术选型

| 层 | 选型 | 说明 |
|----|------|------|
| 小程序 | 原生微信小程序 | 顾客 / 商家分包；底座来自自有空白模板 |
| API | Spring Boot 3 + Java 17 | REST + JSON |
| ORM | MyBatis-Plus | 与表结构映射清晰 |
| 数据库 | MySQL 8 | Compose 内部署 |
| 反向代理 | Nginx | 只反代 `/api/`。图片不经过它 |
| 鉴权 | 微信 `code` → openid → JWT | 无账密；权限看店铺成员 |
| 缓存 | 第一波不上 Redis | JWT + DB 足够 |
| 对象存储 | MinIO | 商品图、头图、头像写入已有桶，公开 URL 给小程序 |
| 构建 | Maven（后端）；微信开发者工具（前端） | — |

### 1.3 仓库结构（目标）

```text
/
├── miniprogram/          # 原生小程序（自模板裁剪）
├── server/               # Spring Boot
├── deploy/               # docker-compose、Nginx、环境样例
├── docs/plans/           # 产品 / 技术方案
├── AGENTS.md
└── README.md
```

### 1.4 部署

- **本地**：`docker compose up` 起 MySQL + API（+ Nginx）
- **上线**：云主机装 Docker，同一套 Compose；证书挂 Nginx
- **环境变量**：`WX_APPID`、`WX_SECRET`、JWT 密钥、DB 密码、单店默认 `DEFAULT_STORE_ID`、店主冷启动 `BOOTSTRAP_OWNER_OPENID`

---

## 2. 领域模型与表结构

金额统一用「分」存整数。除 `user` 外，业务表一律带 `store_id`。

| 表 | 要点 |
|----|------|
| `store` | 店名、联系方式、`contact_note` 购买须知（可多行）、`delivery_remark_phrases` / `pickup_remark_phrases`（配送、自提各自的结算备注快捷短语，换行分隔，各最多 8 句、每句 8 字；空=该履约方式不显示短语）、`min_delivery_fen`（配送起送金额，分，0=不限制；只拦配送提交，按商品合计券前预估比较）、状态、`invite_code`（店员邀请，可轮换）、首页头图 `hero_image_url`（封面，与列表第一张同步）与 `hero_image_urls`（JSON，最多 3 张）、公告 `home_notice`（最多 80 字，空则不展示）、营业文案/主标题/副标题、首页商品列数（1\|2\|3）、点商品打开方式 `product_detail_mode`（SHEET\|PAGE，默认 SHEET）、会员码入口 `member_code_entry`（PROFILE\|TAB，默认 PROFILE）；单店时一条记录 |
| `user` | `openid` 唯一、昵称/头像可选 |
| `store_member` | `user_id + store_id + role`（`OWNER` / `STAFF`）；第一版能力相同 |
| `category` | `store_id`、名称、排序、启用 |
| `product` | `category_id`、名、`image_url`（封面=第一张）、`image_urls`（JSON 数组，最多 3 条 URL）、单位枚举（`PIECE`/`PORTION`/`JIN`）、可选 `spec_note`（规格说明，短文案）、价格分、`in_stock`、上下架、`home_recommended`（首页推荐，默认否）；第二波启用 `stock_qty`/`min_qty`/`max_qty`（`DECIMAL(10,2)`，可空=不限制） |

**单位**：服务端枚举 + 接口下发或前后端共用常量；**禁止**商品表自由字符串单位。  
**规格说明**：`spec_note` 可空，最长 64；用于「约 10 枚」等内容，不替代单位枚举。  
**商品图**：`image_urls` 存完整列表（≤3）；保存时同步 `image_url = image_urls[0]`（空则两字段皆空）。读接口同时返回 `imageUrl` + `imageUrls`；仅有旧 `image_url` 时兼容为单元素 `imageUrls`。列表/篮/分类缩略图只用 `imageUrl`；详情半屏与整页用 `imageUrls` 轮播。  
**分类**：仅通过 `category` 表维护；商品编辑下拉与顾客类目均查本店启用分类。
| `user_address` | 手机、小区、楼栋、室号等；下单时快照进订单 |
| `pickup_slot` | 自提可约时段 + 启用 |
| `cart_item` | `user_id + store_id + product_id + qty`（落库，换机不丢） |
| `order` | 履约方式、状态、地址快照或时段、备注、「尽快」、取消信息、金额合计；自提可加 `customer_arrived_at`（我到店了） |
| `order_item` | 商品名/单位/单价/数量/小计快照 |
| `restock_watch` | `user_id + store_id + product_id`，售罄「到货提醒」；推送成功或用户取消后清理/标记 |

### 2.1 订单状态

```text
PENDING_CONFIRM → PREPARING → DELIVERING | READY_PICKUP → COMPLETED
         ↘_____________ CANCELLED _____________↙
```

- 配送：`DELIVERING`；自提：`READY_PICKUP`
- 任意**未完成**态：顾客或本店成员可取消
- 非法跳转由服务端拒绝
- 状态变更建议带期望原状态（乐观校验），防连点

### 2.2 红点与订阅

- 商家待处理：订单列表按状态排序 + `merchant_unread`（或等价标记）
- 订阅消息：下单、顾客取消、进入配送中时服务端调用；失败只记日志，不阻断主流程
- 不单建复杂消息中心

---

## 3. API、鉴权与权限

### 3.1 约定

- 前缀：`/api/v1`
- 成功/失败：统一包装；错误体 `{ code, message }`
- 请求头：`Authorization: Bearer <jwt>`；可选 `X-Store-Id`（单店可服务端默认）

### 3.2 登录

1. 小程序 `wx.login` 得 `code`
2. `POST /auth/wechat`：用 `wx.login` code 换 openid，无用户则创建，签发 JWT（含 `userId`）；若 openid 命中 `BOOTSTRAP_OWNER_OPENID` 且默认店尚无其他店主，则写入 `store_member.OWNER`
3. JWT 过期 / 未登录：约定错误码，小程序静默重登

### 3.3 接口边界

**顾客**（登录即可）：分类/商品浏览、商品篮、地址簿、提交预约、我的订单、取消未完成单、订单详情；再来一单（服务端按历史明细生成加购，跳过无效商品）；售罄订阅到货提醒；待自提单「我到店了」。客服走微信原生能力，无自建会话 API。

**商家**（路径建议 `/api/v1/merchant/...`，须为该店 `store_member`）：订单列表与状态推进、取消；**分类 CRUD**；商品 CRUD、有货售罄、上下架、首页推荐开关（有货变更时异步通知 `restock_watch`）；自提时段；店铺联系方式与**首页展示**；订单台「马上处理」；**邀请店员** `GET /merchant/invite`（返回 `code` + `inviteUrl` + 普通链接二维码 PNG；链接=`{INVITE_LINK_BASE}?code=`；本地 ZXing 生成，**非**微信太阳码）。公众平台配置「扫普通链接二维码打开小程序」：落地页 `packageCustomer/pages/join-store/join-store`，参数名 `code`。加入页无常规入口、禁分享；`POST /invites/accept`。冷启动 `BOOTSTRAP_OWNER_OPENID` 仍保留。

商品删除是软删除（`product.deleted_at`）。`DELETE /merchant/products/{id}` 删除一个；`POST /merchant/products/batch` 的 body 为 `{ ids, op }`，`op` 取 `ON_SHELF`、`OFF_SHELF`、`IN_STOCK`、`SOLD_OUT`、`RECOMMEND`、`UNRECOMMEND`、`DELETE`，一次最多 100 个。删除后商家列表和顾客在售不再返回该商品，本店商品篮去掉它，未发出的到货订阅取消，未处理的实景申请清掉。订单明细仍保留下单时的名称和价格。

**订阅消息**（模板 ID 详见 `deploy/.env.example` 与 `miniprogram/utils/config.js`；未配置只记日志）：

硬约束：微信**不能**向未授权用户推送；授权须**用户点击**触发（勿依赖 `onShow`）。普通**一次性订阅**≈授权一次可发一条，发出后须再点「开启订单通知」；长期订阅须公众平台类目许可，本项目第一波不依赖。未授权/额度用尽时以**订单台红点**为主通道。

| 环境变量 | 小程序 config 键 | 谁点哪里授权 | 何时发送 | 发给谁 |
|----------|------------------|--------------|----------|--------|
| `WX_SUBSCRIBE_RESTOCK_TEMPLATE_ID` | `restockTemplateId` | 顾客「到货提醒我」 | 改回有货 | 该顾客 |
| `WX_SUBSCRIBE_NEW_ORDER_TEMPLATE_ID` | `newOrderTemplateId` | 店员订单台「开启订单通知」 | 顾客提交预约 | **已授权**的本店成员 |
| `WX_SUBSCRIBE_CANCEL_TEMPLATE_ID` | `cancelTemplateId` | 店员订单台「开启订单通知」或取消前 | 顾客取消订单 | **已授权**的本店成员 |
| `WX_SUBSCRIBE_DELIVERING_TEMPLATE_ID` | `deliveringTemplateId` | 顾客提交上门配送 | 店员推进到「配送中」 | 该顾客 |

发送体：`thing1` 摘要、`thing2` 提示。到货提醒为 `thing1` 商品名、`thing2` 门店名、`thing3`「已到货，可继续预约」。公众平台选用关键词的顺序须与此一致。

**顾客商品列表**：`GET /api/v1/products` 可选 `categoryId`、`homeRecommended=true`（首页推荐区）；始终仅返回已上架。

**元数据**：`GET /api/v1/meta/units` → `[{ code: "JIN", label: "斤" }, …]`（或前端常量与后端校验集一致）。

备注快捷短语：店铺 `delivery_remark_phrases` 与 `pickup_remark_phrases`，随门店设置分开保存；顾客结算按当前履约方式读 `deliveryRemarkPhrases` 或 `pickupRemarkPhrases`，多选拼进 `order.remark`，仍可手改。某一套为空则该方式不展示短语。

**与 UI 对齐**：列表加购、篮角标、提交成功反馈、订单横向状态、订单台状态导航等见产品 §2.8 与 `docs/plans/2026-10-07-ui-prototype.md`，接口需支撑同一套流程。订单台仍用现有看板接口，由客户端按状态分列表。

### 3.4 权限

- 默认身份均为顾客；「商家工作台」入口 = 存在任意 `store_member`
- `OWNER` / `STAFF` 第一版能力相同
- 写操作校验 `store_id` 与成员关系，防跨店越权（403）

### 3.5 多店预留

- 顾客：`browseStoreId`；商家：`manageStoreId`
- 切店只换上下文，不换登录态；第一版可固定唯一店

---

## 4. 小程序工程

### 4.1 底座来源

以自有模板 `/Users/cme/Documents/github/miniprogram` 为骨架，拷入本仓 `miniprogram/`。

**保留**

- `extend/`：劫持 `Page` / `Component`、behaviors、`pageRouter`、trait
- `utils/`：`config` → `api` → `request`（含白名单队列、idle 调度）、`workers`、`storage`、`update` 等
- 基础组件：`Header`、`loading`、`Modal`、`agreement`、`maskElement`、`privacyPopup`
- `glass-easel`、`lazyCodeLoading`、自定义导航、Worker

**去掉 / 不迁入**

- `cloudfunctions/`、`utils/cloud.js` 及一切云开发初始化
- 第二/三波业务页与组件的默认启用（积分、图像选品、收银、近场会员等）——代码可暂不拷或留目录但不进分包页面列表

### 4.2 必改点

| 项 | 从 | 到 |
|----|----|-----|
| 登录 | `cloudCall('login')` | `POST /api/v1/auth/wechat`，JWT 存 storage |
| 请求头 | cookie / `ut` 为主 | `Authorization: Bearer` |
| 域名 | 模板旧 host | Compose/Nginx 提供的 API 域名 |
| 角色 | 全局互斥 `role` | `userInfo` + `isStoreMember` / `manageStoreId`；入口按成员显隐 |

### 4.3 分包

| 包 | 内容 |
|----|------|
| 主包 | 启动、登录编排、公共组件与 utils |
| `packageCustomer` | 首页、分类、商品篮、我的、地址、订单 |
| `packageMerchant` | 订单台、商品、履约/店铺设置 |

模板中已有的 `ProductCard`、`CartBar`、`OrderCard`、`Stepper` 等，第一波可按需接入并改数据源。

### 4.4 与后端协作

- 业务请求只走 `RequestManager` → Java
- 关键节点申请订阅消息授权
- 订单详情等页使用微信客服按钮

---

## 5. 关键时序与异常

### 5.1 冷启动店主

1. 店主微信先登录一次小程序（写入 `user`）
2. 查库得到 openid：`SELECT id, openid, nickname FROM user;`
3. 部署配置 `BOOTSTRAP_OWNER_OPENID=<openid>` 并重启 API
4. 该账号再登录 → 确保其为默认店唯一 `OWNER`（若他人曾是店主则降为 `STAFF`）→「我的」出现商家工作台  

本地与生产同一机制；亦可直接刷库插入 `store_member`。

### 5.2 下单

校验商品上架且有货 → 写 `order` + `order_item` 快照 → 清理对应 `cart_item` → 异步订阅消息（失败不回滚）。

### 5.3 商家推进 / 取消

成员鉴权 → 合法状态转移或取消 → 落库（含取消方/原因/时间）→ 可选通知对端。

### 5.4 异常约定

| 场景 | 处理 |
|------|------|
| 微信换码失败 | 登录失败提示 / 重试 |
| JWT 过期 | 未登录码 + 静默重登 |
| 跨店写 | 403 |
| 售罄提交 | 业务错误码 + 前端提示 |
| 图片上传失败 | 可重试，不挡其它字段；多图单张失败不影响已上传张 |
| 商品图超过 3 张 | 服务端拒绝保存 |
| 订阅消息失败 | 日志，不阻断订单 |

---

## 6. 第一波明确不做（工程侧）

在线支付、Redis、OSS 必选、店员细权限、PC 端、小程序账密、云函数主业务。  
第二/三波（库存扣减、起购限购、积分、图像选品、券、到店补记积分）见 `docs/plans/2026-10-07-wave2-wave3-design.md`。

---

## 7. 验收标准

1. Compose 一键起 API + MySQL（+ Nginx）
2. 小程序微信登录拿到 JWT
3. 店主（刷库）可上架分类/商品、配置自提时段
4. 顾客可加购、维护地址、预约配送与自提各至少一单
5. 店员可将订单推至完成；未完成单双方可取消
6. 商家侧可见待处理优先；客服入口可用
7. 所有业务数据带 `store_id`，无跨店写入

---

## 8. 建议落地顺序（脚手架之后）

1. `server` 空工程 + 健康检查 + Docker Compose + MySQL 迁移  
2. 微信登录 + JWT + `store` / `user` / `store_member`  
3. 自模板裁剪 `miniprogram`，打通登录与请求头  
4. 商品/分类/地址/篮 → 下单与状态机 → 商家订单台  
5. 订阅消息、上传图、红点细节  

---

## 修订记录

| 日期 | 说明 |
|------|------|
| 2026-10-07 | 初稿：架构、表、API、小程序底座、状态机与部署对齐 |
| 2026-10-07 | 对齐顾客增补：再来一单、到货提醒表、我到店了字段 |
| 2026-10-07 | 单位枚举 / 分类 CRUD / UI§2.8 对齐说明 |
| 2026-10-07 | `product.image_urls`（≤3）+ 封面 `image_url` 同步；详情轮播 |
| 2026-10-07 | `product.home_recommended`；顾客 `?homeRecommended=`；商家快捷开关 |
| 2026-10-07 | 第二/三波：指向 wave2-wave3-design；商品数量字段启用 |
| 2026-10-08 | 商品软删除与批量操作：`DELETE /merchant/products/{id}`、`POST /merchant/products/batch` |
| 2026-10-09 | 店铺 `remark_phrases`：结算备注短语按店配置，接口随店铺摘要与门店设置读写 |
| 2026-10-09 | 备注短语拆成 `delivery_remark_phrases` 与 `pickup_remark_phrases`；原列复制到两套后删除 |
| 2026-10-09 | 店铺 `hero_image_urls`（≤3，封面仍写 `hero_image_url`）与 `home_notice`；顾客首页轮播与跑马灯 |
| 2026-10-14 | 订阅：到店模板改为配送出发；顾客取消只通知店员；我到店了只留红点 |
| 2026-10-14 | 到货提醒增加门店名：`thing1` 商品名、`thing2` 门店名、`thing3` 提示 |
| 2026-10-14 | 图片改存 MinIO。Nginx 只反代接口，不再托管 `/uploads/` |
