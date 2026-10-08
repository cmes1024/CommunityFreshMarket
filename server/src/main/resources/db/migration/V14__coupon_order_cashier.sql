-- 预约单挂券（抵扣意向）；收银单挂券并记下券抵扣

ALTER TABLE `order`
  ADD COLUMN coupon_claim_id BIGINT NULL AFTER total_amount_fen,
  ADD COLUMN discount_fen INT NOT NULL DEFAULT 0 AFTER coupon_claim_id;

ALTER TABLE cashier_sale
  ADD COLUMN coupon_claim_id BIGINT NULL AFTER member_user_id,
  ADD COLUMN coupon_discount_fen INT NOT NULL DEFAULT 0 AFTER discount_fen;
