-- 従業員別標準報酬月額を確定前に検証するステージング。

CREATE TABLE IF NOT EXISTS employee_standard_remuneration_input_batch (
    id BIGINT NOT NULL AUTO_INCREMENT,
    source_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    input_by VARCHAR(100) NOT NULL,
    confirmed_by VARCHAR(100) NULL,
    confirmed_at TIMESTAMP(6) NULL,
    change_reason VARCHAR(500) NULL,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    INDEX idx_standard_remuneration_batch_status
        (tenant_id, status, confirmed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS employee_standard_remuneration_input_row (
    id BIGINT NOT NULL AUTO_INCREMENT,
    batch_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    current_record_id BIGINT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE NULL,
    health_standard_remuneration DECIMAL(15, 2) NOT NULL,
    pension_standard_remuneration DECIMAL(15, 2) NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    note VARCHAR(1000) NULL,
    current_effective_from DATE NULL,
    current_effective_to DATE NULL,
    current_health_standard_remuneration DECIMAL(15, 2) NULL,
    current_pension_standard_remuneration DECIMAL(15, 2) NULL,
    current_source_type VARCHAR(30) NULL,
    current_note VARCHAR(1000) NULL,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_standard_remuneration_input_row
        UNIQUE (tenant_id, batch_id, employee_id, effective_from),
    INDEX idx_standard_remuneration_input_batch
        (tenant_id, batch_id),
    CONSTRAINT chk_standard_remuneration_input_period CHECK (
        effective_to IS NULL OR effective_from <= effective_to
    ),
    CONSTRAINT chk_standard_remuneration_input_amount CHECK (
        health_standard_remuneration >= 0
        AND pension_standard_remuneration >= 0
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
