-- 商品实景图：开关、图、拍摄时间；未处理更新申请（同一商品一条）

ALTER TABLE product
  ADD COLUMN live_photo_enabled TINYINT(1) NOT NULL DEFAULT 0,
  ADD COLUMN live_photo_url VARCHAR(512) NULL,
  ADD COLUMN live_photo_taken_at DATETIME(3) NULL;

CREATE TABLE live_photo_request (
  id                 BIGINT      NOT NULL AUTO_INCREMENT,
  store_id           BIGINT      NOT NULL,
  product_id         BIGINT      NOT NULL,
  applicant_count    INT         NOT NULL DEFAULT 0,
  last_requested_at  DATETIME(3) NOT NULL,
  created_at         DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at         DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_live_photo_request_product (product_id),
  KEY idx_live_photo_request_store (store_id, last_requested_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE live_photo_request_user (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  request_id  BIGINT      NOT NULL,
  user_id     BIGINT      NOT NULL,
  created_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_live_photo_request_user (request_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
