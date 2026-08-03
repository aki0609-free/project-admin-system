-- ProjectAdminSystem V1
-- Syncfusion Spreadsheet台帳基盤DDL
-- 本番RDSへは影響確認後に1回だけ実行する。

CREATE TABLE excel_book_data_source_catalog (
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
    CONSTRAINT uk_excel_book_ds_catalog_tenant_code
        UNIQUE (tenant_id, source_code),
    CONSTRAINT ck_excel_book_ds_catalog_max_rows
        CHECK (max_rows BETWEEN 1 AND 10000)
);

CREATE TABLE excel_book_data_source_catalog_column (
    id BIGINT NOT NULL AUTO_INCREMENT,
    catalog_id BIGINT NOT NULL,
    column_name VARCHAR(200) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    data_type VARCHAR(30) NOT NULL,
    order_no INT NOT NULL DEFAULT 1,
    active_flag TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    tenant_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_excel_book_ds_catalog_column
        UNIQUE (tenant_id, catalog_id, column_name),
    CONSTRAINT fk_excel_book_ds_catalog_column_catalog
        FOREIGN KEY (catalog_id)
        REFERENCES excel_book_data_source_catalog (id)
);

CREATE TABLE excel_book_variable_mapping (
    id BIGINT NOT NULL AUTO_INCREMENT,
    master_id BIGINT NOT NULL,
    variable_key VARCHAR(100) NOT NULL,
    source_column VARCHAR(200) NOT NULL,
    scope VARCHAR(20) NOT NULL,
    data_type VARCHAR(30) NOT NULL,
    order_no INT NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    tenant_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_excel_book_variable_mapping
        UNIQUE (tenant_id, master_id, variable_key),
    CONSTRAINT fk_excel_book_variable_mapping_master
        FOREIGN KEY (master_id)
        REFERENCES excel_book_master (id),
    CONSTRAINT ck_excel_book_variable_scope
        CHECK (scope IN ('CONTEXT', 'ROW')),
    CONSTRAINT ck_excel_book_variable_data_type
        CHECK (
            data_type IN (
                'STRING',
                'NUMBER',
                'DATE',
                'DATETIME',
                'BOOLEAN'
            )
        )
);

CREATE INDEX ix_excel_book_ds_catalog_code
    ON excel_book_data_source_catalog (tenant_id, source_code);

CREATE INDEX ix_excel_book_variable_mapping_master
    ON excel_book_variable_mapping (tenant_id, master_id, order_no);

-- physical_nameには台帳専用Viewを指定する。
-- tenant_scoped_flag=1の場合、where_clause_templateには
-- 必ず「tenant_id = :tenantId」を含める。
-- 画面からphysical_nameやwhere_clause_templateは編集させない。
