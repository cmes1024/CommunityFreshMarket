-- 顾客端形态：FULL 完整（价格 + 购物篮 + 下单）/ SHOWCASE 橱窗（仅展示商品，到店购买）
ALTER TABLE store
  ADD COLUMN customer_mode VARCHAR(16) NOT NULL DEFAULT 'FULL';
