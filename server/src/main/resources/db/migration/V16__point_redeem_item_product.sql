-- 兑换物可关联现有商品；为空表示手填

ALTER TABLE point_redeem_item
  ADD COLUMN product_id BIGINT NULL AFTER store_id;
