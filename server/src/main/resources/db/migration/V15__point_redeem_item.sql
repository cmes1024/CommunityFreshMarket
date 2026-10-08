-- 商家配置兑换物；顾客按条目兑换，申请单记下当时的名称和积分

CREATE TABLE point_redeem_item (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  store_id    BIGINT       NOT NULL,
  title       VARCHAR(128) NOT NULL,
  cost_points INT          NOT NULL,
  enabled     TINYINT(1)   NOT NULL DEFAULT 1,
  created_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_point_redeem_item_store (store_id, enabled, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE point_redeem
  ADD COLUMN item_id BIGINT NULL AFTER user_id;
