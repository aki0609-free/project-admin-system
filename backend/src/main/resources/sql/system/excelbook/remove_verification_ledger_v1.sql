-- ProjectAdminSystem V1
-- 開発時に作成した台帳動作確認用マスター／データソースを除去する。
-- 業務用台帳には影響しないよう、専用コードだけを対象にする。

DELETE mapping
FROM excel_book_variable_mapping mapping
JOIN excel_book_master master
  ON master.id = mapping.master_id
 AND master.tenant_id = mapping.tenant_id
WHERE master.tenant_id = 'default'
  AND master.book_code = 'EMPLOYEE_LEDGER_VERIFY';

DELETE FROM excel_book_master
WHERE tenant_id = 'default'
  AND book_code = 'EMPLOYEE_LEDGER_VERIFY';

DELETE catalog_column
FROM excel_book_data_source_catalog_column catalog_column
JOIN excel_book_data_source_catalog catalog
  ON catalog.id = catalog_column.catalog_id
 AND catalog.tenant_id = catalog_column.tenant_id
WHERE catalog.tenant_id = 'default'
  AND catalog.source_code = 'EMPLOYEE_VERIFICATION';

DELETE FROM excel_book_data_source_catalog
WHERE tenant_id = 'default'
  AND source_code = 'EMPLOYEE_VERIFICATION';

DROP VIEW IF EXISTS vw_excel_book_employee_verification;
