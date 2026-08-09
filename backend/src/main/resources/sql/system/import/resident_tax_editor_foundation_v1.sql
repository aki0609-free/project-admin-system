-- 住民税の画面入力・取込を確定前に検証するステージング。

CREATE TABLE IF NOT EXISTS resident_tax_input_batch (
    id BIGINT NOT NULL AUTO_INCREMENT,
    fiscal_year INT NOT NULL,
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
    INDEX idx_resident_tax_input_batch_year_status
        (tenant_id, fiscal_year, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS resident_tax_input_row (
    id BIGINT NOT NULL AUTO_INCREMENT,
    batch_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    month INT NOT NULL,
    tax_amount INT NULL,
    current_tax_amount INT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_resident_tax_input_row
        UNIQUE (tenant_id, batch_id, employee_id, month),
    INDEX idx_resident_tax_input_row_batch (tenant_id, batch_id),
    CONSTRAINT chk_resident_tax_input_month CHECK (month BETWEEN 1 AND 12),
    CONSTRAINT chk_resident_tax_input_amount CHECK (
        tax_amount IS NULL OR tax_amount BETWEEN 0 AND 10000000
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
