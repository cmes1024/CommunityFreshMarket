ALTER TABLE store
  ADD COLUMN hero_image_urls TEXT NULL,
  ADD COLUMN home_notice VARCHAR(80) NULL;

UPDATE store
SET hero_image_urls = JSON_ARRAY(hero_image_url)
WHERE hero_image_url IS NOT NULL AND hero_image_url <> '' AND hero_image_urls IS NULL;
