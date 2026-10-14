-- 购买须知加长；店铺邀请码（成员邀请）
ALTER TABLE store
  ALTER COLUMN contact_note TYPE VARCHAR(1000),
  ADD COLUMN invite_code VARCHAR(16) NULL;

CREATE UNIQUE INDEX uk_store_invite_code ON store (invite_code);
