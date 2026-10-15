UPDATE store SET theme_preset = 'PRODUCE' WHERE theme_preset = 'TEAL';
UPDATE store SET theme_preset = 'BREAKFAST' WHERE theme_preset = 'AMBER';
UPDATE store SET theme_preset = 'EATERY' WHERE theme_preset = 'BRICK';
UPDATE store SET theme_preset = 'MARKET' WHERE theme_preset = 'INK';

ALTER TABLE store ALTER COLUMN theme_preset SET DEFAULT 'PRODUCE';
