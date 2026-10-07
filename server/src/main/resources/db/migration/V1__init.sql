-- Wave-1 schema. Amounts in fen (integer). Business rows carry store_id (except user).

CREATE TABLE store (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  name          VARCHAR(64)  NOT NULL,
  contact_phone VARCHAR(32)  NULL,
  contact_note  VARCHAR(255) NULL,
  status        VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE|DISABLED',
  created_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `user` (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  openid     VARCHAR(64)  NOT NULL,
  nickname   VARCHAR(64)  NULL,
  avatar_url VARCHAR(512) NULL,
  created_at DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_openid (openid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE store_member (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  store_id   BIGINT      NOT NULL,
  user_id    BIGINT      NOT NULL,
  role       VARCHAR(16) NOT NULL COMMENT 'OWNER|STAFF',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_store_member (store_id, user_id),
  KEY idx_store_member_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE category (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  store_id   BIGINT      NOT NULL,
  name       VARCHAR(64) NOT NULL,
  sort_order INT         NOT NULL DEFAULT 0,
  enabled    TINYINT(1)  NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_category_store (store_id, enabled, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE product (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  store_id    BIGINT       NOT NULL,
  category_id BIGINT       NOT NULL,
  name        VARCHAR(128) NOT NULL,
  image_url   VARCHAR(512) NULL,
  unit        VARCHAR(16)  NOT NULL COMMENT 'PIECE|PORTION|JIN',
  price_fen   INT          NOT NULL,
  in_stock    TINYINT(1)   NOT NULL DEFAULT 1,
  on_shelf    TINYINT(1)   NOT NULL DEFAULT 1,
  stock_qty   INT          NULL COMMENT 'reserved for wave 2',
  created_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_product_store_cat (store_id, category_id, on_shelf),
  KEY idx_product_store_shelf (store_id, on_shelf, in_stock)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_address (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  user_id    BIGINT      NOT NULL,
  store_id   BIGINT      NOT NULL,
  phone      VARCHAR(32) NOT NULL,
  community  VARCHAR(64) NOT NULL,
  building   VARCHAR(64) NULL,
  room       VARCHAR(64) NULL,
  is_default TINYINT(1)  NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_address_user_store (user_id, store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE pickup_slot (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  store_id   BIGINT      NOT NULL,
  label      VARCHAR(64) NOT NULL COMMENT 'e.g. 今天 17:00-18:00',
  sort_order INT         NOT NULL DEFAULT 0,
  enabled    TINYINT(1)  NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_pickup_slot_store (store_id, enabled, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cart_item (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  user_id    BIGINT      NOT NULL,
  store_id   BIGINT      NOT NULL,
  product_id BIGINT      NOT NULL,
  qty        DECIMAL(10, 2) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_cart_item (user_id, store_id, product_id),
  KEY idx_cart_user_store (user_id, store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `order` (
  id                   BIGINT       NOT NULL AUTO_INCREMENT,
  store_id             BIGINT       NOT NULL,
  user_id              BIGINT       NOT NULL,
  fulfillment          VARCHAR(16)  NOT NULL COMMENT 'DELIVERY|PICKUP',
  status               VARCHAR(32)  NOT NULL COMMENT 'PENDING_CONFIRM|PREPARING|DELIVERING|READY_PICKUP|COMPLETED|CANCELLED',
  asap                 TINYINT(1)   NOT NULL DEFAULT 1,
  remark               VARCHAR(512) NULL,
  address_snapshot     JSON         NULL,
  pickup_slot_id       BIGINT       NULL,
  pickup_slot_label    VARCHAR(64)  NULL,
  total_amount_fen     INT          NOT NULL DEFAULT 0,
  merchant_unread      TINYINT(1)   NOT NULL DEFAULT 1,
  customer_arrived_at  DATETIME(3)  NULL,
  cancel_by            VARCHAR(16)  NULL COMMENT 'CUSTOMER|MERCHANT',
  cancel_reason        VARCHAR(255) NULL,
  cancelled_at         DATETIME(3)  NULL,
  created_at           DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at           DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_order_store_status (store_id, status, created_at),
  KEY idx_order_user (user_id, created_at),
  KEY idx_order_merchant_unread (store_id, merchant_unread, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE order_item (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  order_id        BIGINT       NOT NULL,
  store_id        BIGINT       NOT NULL,
  product_id      BIGINT       NULL,
  product_name    VARCHAR(128) NOT NULL,
  unit            VARCHAR(16)  NOT NULL,
  unit_price_fen  INT          NOT NULL,
  qty             DECIMAL(10, 2) NOT NULL,
  amount_fen      INT          NOT NULL,
  created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_order_item_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE restock_watch (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  user_id      BIGINT      NOT NULL,
  store_id     BIGINT      NOT NULL,
  product_id   BIGINT      NOT NULL,
  status       VARCHAR(16) NOT NULL DEFAULT 'WATCHING' COMMENT 'WATCHING|NOTIFIED|CANCELLED',
  created_at   DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  notified_at  DATETIME(3) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_restock_user_product (user_id, store_id, product_id),
  KEY idx_restock_product (store_id, product_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Single-store seed for local / first deploy
INSERT INTO store (id, name, contact_phone, contact_note, status)
VALUES (1, '绿意鲜生', '13800000000', '社区配送 / 到店自提', 'ACTIVE');

INSERT INTO category (store_id, name, sort_order, enabled) VALUES
  (1, '叶菜', 1, 1),
  (1, '根茎', 2, 1),
  (1, '水果', 3, 1),
  (1, '蛋奶', 4, 1);

INSERT INTO pickup_slot (store_id, label, sort_order, enabled) VALUES
  (1, '今天 17:00-18:00', 1, 1),
  (1, '今天 18:00-19:00', 2, 1),
  (1, '明天 10:00-11:00', 3, 1);
