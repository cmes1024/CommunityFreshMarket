ALTER TABLE product
  ADD COLUMN image_urls JSON NULL COMMENT '商品图 URL 列表，最多 3 张；image_url 为封面=第一张' AFTER image_url;

UPDATE product
SET image_urls = JSON_ARRAY(image_url)
WHERE image_url IS NOT NULL AND image_url <> '' AND image_urls IS NULL;
