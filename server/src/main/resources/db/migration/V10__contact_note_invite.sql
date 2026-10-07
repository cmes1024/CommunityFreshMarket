-- 购买须知加长；店铺邀请码（成员邀请）
ALTER TABLE store
  MODIFY COLUMN contact_note VARCHAR(1000) NULL COMMENT '购买须知，可空',
  ADD COLUMN invite_code VARCHAR(16) NULL COMMENT '店员邀请码，可轮换' AFTER status;

ALTER TABLE store
  ADD UNIQUE KEY uk_store_invite_code (invite_code);
