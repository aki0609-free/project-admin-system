-- 業務固有のマスターを、定義とデータに分けて汎用的に管理する。

CREATE TABLE IF NOT EXISTS generic_master_definition (
    id BIGINT NOT NULL AUTO_INCREMENT,
    master_code VARCHAR(100) NOT NULL,
    master_name VARCHAR(200) NOT NULL,
    tab_name VARCHAR(100) NOT NULL,
    placement VARCHAR(40) NOT NULL DEFAULT 'BUSINESS_SETTINGS',
    description VARCHAR(1000) NULL,
    display_order INT NOT NULL DEFAULT 100,
    definition_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    active_flag BOOLEAN NOT NULL DEFAULT TRUE,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_generic_master_definition_code UNIQUE (tenant_id, master_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS generic_master_field_definition (
    id BIGINT NOT NULL AUTO_INCREMENT,
    definition_id BIGINT NOT NULL,
    field_key VARCHAR(100) NOT NULL,
    field_label VARCHAR(200) NOT NULL,
    field_type VARCHAR(30) NOT NULL,
    required_flag BOOLEAN NOT NULL DEFAULT FALSE,
    list_visible_flag BOOLEAN NOT NULL DEFAULT TRUE,
    searchable_flag BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 100,
    unit VARCHAR(50) NULL,
    placeholder VARCHAR(500) NULL,
    validation_pattern VARCHAR(1000) NULL,
    options_json TEXT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_generic_master_field_key UNIQUE (tenant_id, definition_id, field_key),
    CONSTRAINT fk_generic_master_field_definition
        FOREIGN KEY (definition_id) REFERENCES generic_master_definition (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS generic_master_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    definition_id BIGINT NOT NULL,
    values_json JSON NOT NULL,
    active_flag BOOLEAN NOT NULL DEFAULT TRUE,
    lock_version BIGINT NOT NULL DEFAULT 0,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    INDEX idx_generic_master_record_definition (tenant_id, definition_id, deleted_at),
    CONSTRAINT fk_generic_master_record_definition
        FOREIGN KEY (definition_id) REFERENCES generic_master_definition (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
