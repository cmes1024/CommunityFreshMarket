INSERT INTO product (store_id, category_id, name, image_url, unit, price_fen, in_stock, on_shelf)
SELECT 1, c.id, v.name, NULL, v.unit, v.price_fen, v.in_stock, 1
FROM (
  SELECT '叶菜' AS cat, '小白菜' AS name, 'JIN' AS unit, 398 AS price_fen, 1 AS in_stock
  UNION ALL SELECT '叶菜', '生菜', 'JIN', 498, 1
  UNION ALL SELECT '根茎', '土豆', 'JIN', 298, 1
  UNION ALL SELECT '水果', '香蕉', 'JIN', 598, 0
  UNION ALL SELECT '蛋奶', '土鸡蛋', 'PORTION', 1580, 1
) v
JOIN category c ON c.store_id = 1 AND c.name = v.cat;
