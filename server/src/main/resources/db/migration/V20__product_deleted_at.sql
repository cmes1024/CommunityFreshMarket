ALTER TABLE product
  ADD COLUMN deleted_at DATETIME(3) NULL AFTER updated_at,
  ADD KEY idx_product_store_deleted (store_id, deleted_at);
