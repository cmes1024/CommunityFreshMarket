# 第二波 / 第三波 — 技术设计（落地稿）

日期：2026-10-07  
状态：按产品轮廓锁默认决策后开工；可事后微调规则，不必阻塞实现。

---

## 0. 本会话默认决策（无需产品再拍板）

| 议题 | 默认 |
|------|------|
| 库存 `stock_qty` | `NULL`=不限数量（仍看 `in_stock`）；有值则硬卡；数量用 `DECIMAL(10,2)` 与篮一致 |
| 扣减时机 | **提交预约单成功时**扣减；**取消**回滚；扣到 0 → 自动 `in_stock=false`；回滚后若库存>0 → 自动 `in_stock=true` |
| 起购/限购 | `min_qty`/`max_qty` 可空；与库存并存取更严；作用于加购/改数量/再来一单 |
| 积分比例 | 店铺 `points_per_yuan`（默认 1）：每满 100 分金额入账 `floor(totalFen/100)*比例`；**订单 COMPLETED 时**入账一次 |
| 积分兑换 | 先「兑换申请 + 线下履约」；商家确认核销；不做线上支付抵扣 |
| 商品实景图 | 见 `2026-10-08-live-photo-design.md`。商品级开关默认关；详情/半屏第一张为实景图；超时可申请更新；站内信合并计数；相册或相机提交后申请消失。有效时长仍用 `pick_image_ttl_minutes`（默认 15 分钟）。不再做打标、一图一锁、确认进篮 |
| 第二波可选 | **不做**店长时令手写、缺货替代 |
| 优惠券 | 自建满减/固定面额；领取后下单可挂「抵扣意向」；核销记流水；不接微信卡券 |
| 收银 | 店员建收银单、扫码/选商品、挂会员、临时改价落库；状态完成触发积分；收款线下 |
| 快速报到 | 顾客主动报到，有效 **2 分钟**；店员列表点选挂到收银；附近 500m 列表可后补 GPS，先做主动报到 |

---

## 1. 第二波表与字段

### 1.1 商品扩展

`product` 增加/调整：

- `stock_qty DECIMAL(10,2) NULL`（原 INT 升级）
- `min_qty DECIMAL(10,2) NULL`
- `max_qty DECIMAL(10,2) NULL`

### 1.2 店铺积分配置

`store` 增加：

- `points_per_yuan INT NOT NULL DEFAULT 1`（0=关闭积分）
- `pick_image_ttl_minutes INT NOT NULL DEFAULT 15`

### 1.3 积分

```text
point_account (user_id, store_id, balance, updated_at) UNIQUE(user_id, store_id)
point_ledger  (id, store_id, user_id, delta, balance_after, reason, ref_type, ref_id, remark, created_at)
point_redeem  (id, store_id, user_id, cost_points, title, status PENDING|DONE|CANCELLED, created_at, updated_at)
```

`reason`：`ORDER_COMPLETE` / `CASHIER_COMPLETE` / `REDEEM` / `ADJUST` / `CANCEL_REVERSAL`

### 1.4 商品实景图

表和接口以 `2026-10-08-live-photo-design.md` 为准。`pick_session` 及打标、锁定、确认进篮不再使用。

---

## 2. 第三波表（骨架）

```text
coupon (id, store_id, title, type FIXED|AMOUNT_OFF, face_fen, min_spend_fen, total/limit_per_user, starts_at, ends_at, enabled)
coupon_claim (id, coupon_id, store_id, user_id, status UNUSED|RESERVED|USED|EXPIRED, order_id/cashier_id, claimed_at, used_at)

cashier_sale (id, store_id, member_user_id NULL, status OPEN|COMPLETED|CANCELLED,
  total_fen, discount_fen, pay_fen, adjust_reason, created_by, completed_at, ...)
cashier_sale_item (id, sale_id, product_id, name, unit, unit_price_fen, qty, amount_fen, adjusted)

member_checkin (id, store_id, user_id, expires_at, consumed_at NULL, lat/lng optional)
```

---

## 3. API 摘要

### 第二波

- 商品读写带 `stockQty`/`minQty`/`maxQty`
- 篮/下单：服务端校验起购限购库存；提交扣减；取消回滚
- `GET /api/v1/points/me` 余额+最近明细
- `POST /api/v1/points/redeem` `{ title, costPoints }`
- `GET|PUT` 商家店铺设置含 `pointsPerYuan`、`pickImageTtlMinutes`
- 商家：`GET /api/v1/merchant/points/redeems`、`POST .../redeems/{id}/complete`
- 实景图：见 `2026-10-08-live-photo-design.md`（申请、站内信、提交新图）

### 第三波

- 券 CRUD / 领取 / 下单挂券
- 收银单 CRUD + 完成
- `POST /checkins` 快速报到；商家 `GET /merchant/checkins/active`

---

## 4. 验收（本波次）

第二波：商家可配库存与起购限购；超限加购被拒；下单扣库存、取消回滚；完成单入积分；顾客可见余额并可申请兑换；商品实景图按 `2026-10-08-live-photo-design.md` 可走通。  
第三波：可建券领取；可开收银单完成；顾客可报到且店员可见有效名单。

---

## 修订记录

| 日期 | 说明 |
|------|------|
| 2026-10-07 | 首稿：锁默认决策 + 表/API 轮廓，供实现 |
| 2026-10-08 | 图像选品改为商品实景图，细则见 `2026-10-08-live-photo-design.md` |
