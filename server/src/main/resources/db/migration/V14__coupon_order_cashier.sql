-- 预约单挂券（抵扣意向）；收银单挂券并记下券抵扣

ALTER TABLE "order"
  ADD COLUMN coupon_claim_id BIGINT NULL,
  ADD COLUMN discount_fen INT NOT NULL DEFAULT 0;

ALTER TABLE cashier_sale
  ADD COLUMN coupon_claim_id BIGINT NULL,
  ADD COLUMN coupon_discount_fen INT NOT NULL DEFAULT 0;
