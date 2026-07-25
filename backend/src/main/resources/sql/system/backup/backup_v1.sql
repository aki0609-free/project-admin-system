-- ProjectAdminSystem V1: database CSV backup hardening
-- Apply once after checking existing indexes and duplicate rows.

-- The former Hibernate schema may have a global UNIQUE index on target_code.
-- Inspect SHOW INDEX FROM backup_target and remove that single-column UNIQUE
-- index before adding the tenant-scoped constraint below.
ALTER TABLE backup_target
    ADD CONSTRAINT uk_backup_target_tenant_code
        UNIQUE (tenant_id, target_code);

ALTER TABLE backup_column
    ADD CONSTRAINT uk_backup_column_target_column
        UNIQUE (target_id, column_name),
    ADD CONSTRAINT uk_backup_column_target_order
        UNIQUE (target_id, order_no);

ALTER TABLE backup_history
    ADD INDEX idx_backup_history_tenant_status
        (tenant_id, status),
    ADD INDEX idx_backup_history_tenant_executed
        (tenant_id, executed_at);

-- A backup target referenced by a BACKUP batch must persist its output.
-- Review the intended output_dir before applying an update such as:
--
-- UPDATE backup_target target
-- INNER JOIN batch_job_definition job
--         ON job.tenant_id = target.tenant_id
--        AND job.target_code = target.target_code
-- SET target.output_mode = 'SERVER_FILE',
--     target.output_dir = 'master-data'
-- WHERE job.job_type = 'BACKUP'
--   AND target.output_mode = 'DOWNLOAD'
--   AND target.deleted_at IS NULL
--   AND job.deleted_at IS NULL;
