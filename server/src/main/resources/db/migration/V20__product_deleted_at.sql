ALTER TABLE product
  ADD COLUMN deleted_at TIMESTAMP(3) NULL;

CREATE INDEX idx_product_store_deleted ON product (store_id, deleted_at);
