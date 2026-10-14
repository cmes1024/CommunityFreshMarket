ALTER TABLE product
  ADD COLUMN home_recommended BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_product_store_home ON product (store_id, on_shelf, home_recommended);
