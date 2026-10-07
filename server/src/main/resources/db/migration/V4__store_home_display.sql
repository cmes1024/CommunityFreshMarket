ALTER TABLE store
  ADD COLUMN home_kicker VARCHAR(64) NULL COMMENT '顾客首页营业状态文案，空则用客户端默认' AFTER hero_image_url,
  ADD COLUMN home_subtitle VARCHAR(128) NULL COMMENT '顾客首页副标题，空则用客户端默认' AFTER home_kicker,
  ADD COLUMN home_product_cols TINYINT NOT NULL DEFAULT 2 COMMENT '首页商品列数 1|2|3' AFTER home_subtitle;
