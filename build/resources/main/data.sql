INSERT INTO product (id, name, price, stock)
SELECT * FROM (SELECT 1, 'Keyboard', 45000, 100) AS tmp
WHERE NOT EXISTS (SELECT id FROM product WHERE id = 1);

INSERT INTO product (id, name, price, stock)
SELECT * FROM (SELECT 2, 'Mouse', 25000, 200) AS tmp
WHERE NOT EXISTS (SELECT id FROM product WHERE id = 2);

INSERT INTO product (id, name, price, stock)
SELECT * FROM (SELECT 3, 'Monitor', 250000, 50) AS tmp
WHERE NOT EXISTS (SELECT id FROM product WHERE id = 3);
