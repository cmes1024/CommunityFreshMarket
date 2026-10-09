ALTER TABLE store
  ADD COLUMN hero_image_urls JSON NULL COMMENT '首页头图，最多 3 张；hero_image_url 为第一张' AFTER hero_image_url,
  ADD COLUMN home_notice VARCHAR(80) NULL COMMENT '首页公告，空则不展示' AFTER home_subtitle;

UPDATE store
SET hero_image_urls = JSON_ARRAY(hero_image_url)
WHERE hero_image_url IS NOT NULL AND hero_image_url <> '' AND hero_image_urls IS NULL;
