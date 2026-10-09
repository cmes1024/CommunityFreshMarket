ALTER TABLE store
  ADD COLUMN remark_phrases VARCHAR(160) NOT NULL DEFAULT ''
    COMMENT '结算备注快捷短语，换行分隔，最多 8 句、每句 8 字' AFTER contact_note;

UPDATE store
SET remark_phrases = CONCAT('放门口', CHAR(10), '少葱', CHAR(10), '称重点', CHAR(10), '不要电话')
WHERE remark_phrases = '';
