SELECT COUNT(*) AS application_table_count
FROM information_schema.tables
WHERE table_schema = 'ADMIN'
  AND table_type = 'BASE TABLE';

SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'ADMIN'
  AND table_name LIKE 'BATCH\_%'
ORDER BY table_name;

SHOW GRANTS FOR 'projectadmin_app'@'10.20.0.%';
