ALTER TABLE store
  ADD COLUMN product_detail_mode VARCHAR(16) NOT NULL DEFAULT 'SHEET'
    COMMENT '顾客点商品：SHEET 半屏 | PAGE 整页详情' AFTER home_product_cols;
