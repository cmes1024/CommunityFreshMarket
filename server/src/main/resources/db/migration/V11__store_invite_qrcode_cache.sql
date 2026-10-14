-- 缓存邀请小程序码，避免每次打开都调微信接口（getwxacodeunlimit 有调用频率限制）
ALTER TABLE store
  ADD COLUMN invite_qrcode_base64 TEXT NULL;
