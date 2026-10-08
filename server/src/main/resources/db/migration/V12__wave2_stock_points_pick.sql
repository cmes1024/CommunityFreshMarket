-- Wave 2: stock / min-max qty / points / pick session
-- Wave 3 skeleton: coupon / cashier / checkin

ALTER TABLE product
  MODIFY COLUMN stock_qty DECIMAL(10, 2) NULL COMMENT 'NULL=unlimited qty; 0 => sold out when tracked',
  ADD COLUMN min_qty DECIMAL(10, 2) NULL COMMENT 'NULL=no min' AFTER stock_qty,
  ADD COLUMN max_qty DECIMAL(10, 2) NULL COMMENT 'NULL=no max' AFTER min_qty;

ALTER TABLE store
  ADD COLUMN points_per_yuan INT NOT NULL DEFAULT 1 COMMENT '0=points off; earn floor(fen/100)*ratio on complete',
  ADD COLUMN pick_image_ttl_minutes INT NOT NULL DEFAULT 15;

CREATE TABLE point_account (
  user_id    BIGINT       NOT NULL,
  store_id   BIGINT       NOT NULL,
  balance    INT          NOT NULL DEFAULT 0,
  updated_at DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (user_id, store_id),
  KEY idx_point_account_store (store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE point_ledger (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  store_id      BIGINT       NOT NULL,
  user_id       BIGINT       NOT NULL,
  delta         INT          NOT NULL,
  balance_after INT          NOT NULL,
  reason        VARCHAR(32)  NOT NULL,
  ref_type      VARCHAR(32)  NULL,
  ref_id        BIGINT       NULL,
  remark        VARCHAR(256) NULL,
  created_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_point_ledger_user (store_id, user_id, id),
  KEY idx_point_ledger_ref (store_id, ref_type, ref_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE point_redeem (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  store_id    BIGINT       NOT NULL,
  user_id     BIGINT       NOT NULL,
  cost_points INT          NOT NULL,
  title       VARCHAR(128) NOT NULL,
  status      VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
  created_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_point_redeem_store_status (store_id, status, id),
  KEY idx_point_redeem_user (store_id, user_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE pick_session (
  id                  BIGINT        NOT NULL AUTO_INCREMENT,
  store_id            BIGINT        NOT NULL,
  product_id          BIGINT        NOT NULL,
  customer_user_id    BIGINT        NOT NULL,
  status              VARCHAR(24)   NOT NULL,
  image_url           VARCHAR(512)  NULL,
  expires_at          DATETIME(3)   NULL,
  lock_holder_user_id BIGINT        NULL,
  marks_json          TEXT          NULL,
  confirmed_qty       DECIMAL(10,2) NULL,
  order_id            BIGINT        NULL,
  created_at          DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at          DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_pick_product_active (store_id, product_id, status),
  KEY idx_pick_customer (store_id, customer_user_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE coupon (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  store_id        BIGINT       NOT NULL,
  title           VARCHAR(64)  NOT NULL,
  type            VARCHAR(16)  NOT NULL COMMENT 'FIXED|AMOUNT_OFF',
  face_fen        INT          NOT NULL,
  min_spend_fen   INT          NOT NULL DEFAULT 0,
  total_limit     INT          NULL,
  per_user_limit  INT          NOT NULL DEFAULT 1,
  claimed_count   INT          NOT NULL DEFAULT 0,
  starts_at       DATETIME(3)  NULL,
  ends_at         DATETIME(3)  NULL,
  enabled         TINYINT(1)   NOT NULL DEFAULT 1,
  created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_coupon_store (store_id, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE coupon_claim (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  coupon_id  BIGINT       NOT NULL,
  store_id   BIGINT       NOT NULL,
  user_id    BIGINT       NOT NULL,
  status     VARCHAR(16)  NOT NULL DEFAULT 'UNUSED',
  order_id   BIGINT       NULL,
  cashier_id BIGINT       NULL,
  claimed_at DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  used_at    DATETIME(3)  NULL,
  PRIMARY KEY (id),
  KEY idx_claim_user (store_id, user_id, status),
  KEY idx_claim_coupon (coupon_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cashier_sale (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  store_id        BIGINT       NOT NULL,
  member_user_id  BIGINT       NULL,
  status          VARCHAR(16)  NOT NULL DEFAULT 'OPEN',
  total_fen       INT          NOT NULL DEFAULT 0,
  discount_fen    INT          NOT NULL DEFAULT 0,
  pay_fen         INT          NOT NULL DEFAULT 0,
  adjust_reason   VARCHAR(128) NULL,
  created_by      BIGINT       NOT NULL,
  completed_at    DATETIME(3)  NULL,
  created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_cashier_store (store_id, status, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cashier_sale_item (
  id              BIGINT        NOT NULL AUTO_INCREMENT,
  sale_id         BIGINT        NOT NULL,
  store_id        BIGINT        NOT NULL,
  product_id      BIGINT        NULL,
  product_name    VARCHAR(128)  NOT NULL,
  unit            VARCHAR(16)   NOT NULL,
  unit_price_fen  INT           NOT NULL,
  qty             DECIMAL(10,2) NOT NULL,
  amount_fen      INT           NOT NULL,
  adjusted        TINYINT(1)    NOT NULL DEFAULT 0,
  created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_cashier_item_sale (sale_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE member_checkin (
  id          BIGINT        NOT NULL AUTO_INCREMENT,
  store_id    BIGINT        NOT NULL,
  user_id     BIGINT        NOT NULL,
  expires_at  DATETIME(3)   NOT NULL,
  consumed_at DATETIME(3)   NULL,
  lat         DECIMAL(10,6) NULL,
  lng         DECIMAL(10,6) NULL,
  created_at  DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_checkin_active (store_id, expires_at, consumed_at),
  KEY idx_checkin_user (store_id, user_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
