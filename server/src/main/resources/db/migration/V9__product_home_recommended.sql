ALTER TABLE product
  ADD COLUMN home_recommended TINYINT(1) NOT NULL DEFAULT 0
    COMMENT 'homepage recommended; still requires on_shelf' AFTER on_shelf,
  ADD KEY idx_product_store_home (store_id, on_shelf, home_recommended);
