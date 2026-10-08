ALTER TABLE coupon
  ADD COLUMN claim_ends_at DATETIME(3) NULL AFTER starts_at;
