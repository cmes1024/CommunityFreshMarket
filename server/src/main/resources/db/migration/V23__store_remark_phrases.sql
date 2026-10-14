ALTER TABLE store
  ADD COLUMN remark_phrases VARCHAR(160) NOT NULL DEFAULT '';

UPDATE store
SET remark_phrases = concat('放门口', chr(10), '少葱', chr(10), '称重点', chr(10), '不要电话')
WHERE remark_phrases = '';
