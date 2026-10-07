ALTER TABLE product
  ADD COLUMN spec_note VARCHAR(64) NULL COMMENT '规格说明，如约10枚' AFTER unit;

UPDATE product
SET spec_note = '约 10 枚'
WHERE store_id = 1 AND name = '土鸡蛋' AND (spec_note IS NULL OR spec_note = '');
