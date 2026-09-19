ALTER TABLE report_master
MODIFY pre_process_sql LONGTEXT NULL;

ALTER TABLE report_master
MODIFY query_sql LONGTEXT NULL;

ALTER TABLE report_master
MODIFY cleanup_sql LONGTEXT NULL;

ALTER TABLE report_history
MODIFY notes LONGTEXT NULL;

ALTER TABLE report_history
MODIFY request_params_json LONGTEXT NULL;