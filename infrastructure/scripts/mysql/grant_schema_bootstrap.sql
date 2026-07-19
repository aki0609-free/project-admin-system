GRANT
  CREATE,
  ALTER,
  INDEX,
  REFERENCES
ON ADMIN.*
TO 'projectadmin_app'@'10.20.0.%';

SHOW GRANTS FOR 'projectadmin_app'@'10.20.0.%';
