CREATE TABLE balance_account (
  user_id     BIGINT       NOT NULL,
  store_id    BIGINT       NOT NULL,
  balance_fen INT          NOT NULL DEFAULT 0,
  updated_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (user_id, store_id),
  KEY idx_balance_account_store (store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE balance_ledger (
  id                 BIGINT       NOT NULL AUTO_INCREMENT,
  store_id           BIGINT       NOT NULL,
  user_id            BIGINT       NOT NULL,
  delta_fen          INT          NOT NULL,
  balance_after_fen  INT          NOT NULL,
  reason             VARCHAR(16)  NOT NULL COMMENT 'TOPUP|SPEND|REFUND',
  ref_type           VARCHAR(16)  NULL,
  ref_id             BIGINT       NULL,
  operator_user_id   BIGINT       NULL,
  remark             VARCHAR(128) NULL,
  created_at         DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_balance_ledger_user (store_id, user_id, id),
  KEY idx_balance_ledger_ref (store_id, ref_type, ref_id, reason)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE `order`
  ADD COLUMN balance_fen INT NOT NULL DEFAULT 0 AFTER discount_fen;
