REVOKE
  CREATE,
  ALTER,
  INDEX,
  REFERENCES
ON ADMIN.*
FROM 'projectadmin_app'@'10.20.0.%';

SHOW GRANTS FOR 'projectadmin_app'@'10.20.0.%';
