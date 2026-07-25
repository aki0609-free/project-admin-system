-- ProjectAdminSystem V1: batch execution history hardening
-- Apply once when upgrading an existing database.

ALTER TABLE batch_execution_log
    ADD COLUMN trigger_type VARCHAR(30) NULL AFTER status,
    ADD COLUMN executed_by VARCHAR(100) NULL AFTER trigger_type,
    ADD COLUMN parameters_json TEXT NULL AFTER executed_by,
    ADD COLUMN retry_source_log_id BIGINT NULL AFTER parameters_json;

UPDATE batch_execution_log
SET trigger_type = 'MANUAL'
WHERE trigger_type IS NULL;

UPDATE batch_execution_log
SET executed_by = 'legacy'
WHERE executed_by IS NULL;

ALTER TABLE batch_execution_log
    MODIFY COLUMN trigger_type VARCHAR(30) NOT NULL,
    MODIFY COLUMN executed_by VARCHAR(100) NOT NULL,
    ADD INDEX idx_batch_execution_log_job_started (tenant_id, job_code, started_at),
    ADD INDEX idx_batch_execution_log_status_started (tenant_id, status, started_at),
    ADD INDEX idx_batch_execution_log_retry_source (retry_source_log_id);

-- Hibernate ddl-auto=update may leave the former global unique index on job_code.
-- Before adding this index, inspect and remove that old single-column unique index.
ALTER TABLE batch_job_definition
    ADD CONSTRAINT uk_batch_job_definition_tenant_code
        UNIQUE (tenant_id, job_code);
