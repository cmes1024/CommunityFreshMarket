ALTER TABLE product
  ADD COLUMN image_urls TEXT NULL;

UPDATE product
SET image_urls = JSON_ARRAY(image_url)
WHERE image_url IS NOT NULL AND image_url <> '' AND image_urls IS NULL;
