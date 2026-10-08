ALTER TABLE store
  ADD COLUMN min_delivery_fen INT NOT NULL DEFAULT 0 COMMENT '配送起送金额（分），0=不限制' AFTER contact_note;
