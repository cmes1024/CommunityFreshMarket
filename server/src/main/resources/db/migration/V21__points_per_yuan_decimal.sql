ALTER TABLE store
  MODIFY COLUMN points_per_yuan DECIMAL(5, 2) NOT NULL DEFAULT 1.00
  COMMENT '0=points off; earn floor(floor(fen/100)*ratio), up to 2 decimals';
