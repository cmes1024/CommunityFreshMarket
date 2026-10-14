ALTER TABLE store
  ADD COLUMN delivery_remark_phrases VARCHAR(160) NOT NULL DEFAULT '',
  ADD COLUMN pickup_remark_phrases VARCHAR(160) NOT NULL DEFAULT '';

UPDATE store
SET delivery_remark_phrases = remark_phrases,
    pickup_remark_phrases = remark_phrases;

ALTER TABLE store DROP COLUMN remark_phrases;
