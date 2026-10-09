ALTER TABLE store
  ADD COLUMN delivery_remark_phrases VARCHAR(160) NOT NULL DEFAULT ''
    COMMENT '配送结算备注快捷短语，换行分隔，最多 8 句、每句 8 字' AFTER remark_phrases,
  ADD COLUMN pickup_remark_phrases VARCHAR(160) NOT NULL DEFAULT ''
    COMMENT '自提结算备注快捷短语，换行分隔，最多 8 句、每句 8 字' AFTER delivery_remark_phrases;

UPDATE store
SET delivery_remark_phrases = remark_phrases,
    pickup_remark_phrases = remark_phrases;

ALTER TABLE store DROP COLUMN remark_phrases;
