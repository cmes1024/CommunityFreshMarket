ALTER TABLE store
  ALTER COLUMN points_per_yuan TYPE DECIMAL(5, 2) USING points_per_yuan::decimal,
  ALTER COLUMN points_per_yuan SET DEFAULT 1.00;
