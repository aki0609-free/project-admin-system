-- ProjectAdminSystem V1
-- Ruleデータソースカタログ導入DDL
-- Hibernate ddl-auto=updateを利用しない環境で1回だけ実行する。

CREATE TABLE rule_data_source_catalog (
    id BIGINT NOT NULL AUTO_INCREMENT,
    source_code VARCHAR(100) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    physical_name VARCHAR(200) NOT NULL,
    where_clause_template VARCHAR(1000) NULL,
    tenant_scoped_flag TINYINT(1) NOT NULL DEFAULT 1,
    max_rows INT NOT NULL DEFAULT 1000,
    description VARCHAR(1000) NULL,
    active_flag TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    tenant_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_rule_ds_catalog_tenant_code
        UNIQUE (tenant_id, source_code),
    CONSTRAINT ck_rule_ds_catalog_max_rows
        CHECK (max_rows BETWEEN 1 AND 1000)
);

CREATE TABLE rule_data_source_catalog_column (
    id BIGINT NOT NULL AUTO_INCREMENT,
    catalog_id BIGINT NOT NULL,
    column_name VARCHAR(200) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    data_type VARCHAR(50) NOT NULL,
    order_no INT NOT NULL DEFAULT 1,
    active_flag TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    tenant_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_rule_ds_catalog_column
        UNIQUE (tenant_id, catalog_id, column_name),
    CONSTRAINT fk_rule_ds_catalog_column_catalog
        FOREIGN KEY (catalog_id)
        REFERENCES rule_data_source_catalog (id)
);

ALTER TABLE rule_data_source
    ADD COLUMN catalog_code VARCHAR(100) NULL
        AFTER source_name;

CREATE INDEX ix_rule_data_source_catalog_code
    ON rule_data_source (tenant_id, catalog_code);

-- カタログのphysical_nameにはRule専用Viewを指定する。
-- tenant_scoped_flag=1の場合、where_clause_templateには
-- 必ず「tenant_id = :tenantId」を含める。
--
-- 例:
-- INSERT INTO rule_data_source_catalog (
--     source_code,
--     display_name,
--     physical_name,
--     where_clause_template,
--     tenant_scoped_flag,
--     max_rows,
--     description,
--     active_flag,
--     created_at,
--     updated_at,
--     tenant_id
-- ) VALUES (
--     'EMPLOYEE_BASIC',
--     '従業員基本情報',
--     'vw_rule_employee_basic',
--     'tenant_id = :tenantId AND employee_id = :employeeId',
--     1,
--     100,
--     'Rule計算で利用できる従業員基本情報',
--     1,
--     CURRENT_TIMESTAMP(6),
--     CURRENT_TIMESTAMP(6),
--     'default'
-- );
