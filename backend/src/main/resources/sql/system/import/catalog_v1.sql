-- ProjectAdminSystem V1
-- 外部データ取込先カタログ導入DDL
-- init2.sql と column_def.sql の実行後に1回実行する。

CREATE TABLE import_target_catalog (
    id BIGINT NOT NULL AUTO_INCREMENT,
    table_name VARCHAR(200) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    description VARCHAR(1000) NULL,
    tenant_scoped_flag TINYINT(1) NOT NULL DEFAULT 1,
    allow_delete_insert_flag TINYINT(1) NOT NULL DEFAULT 0,
    active_flag TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    tenant_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_import_target_catalog_tenant_table
        UNIQUE (tenant_id, table_name)
);

CREATE TABLE import_target_catalog_column (
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
    CONSTRAINT uk_import_target_catalog_column
        UNIQUE (tenant_id, catalog_id, column_name),
    CONSTRAINT fk_import_target_catalog_column_catalog
        FOREIGN KEY (catalog_id)
        REFERENCES import_target_catalog (id)
);

-- 既存の取込定義を初期カタログとして登録する。
-- カタログ登録後は、管理画面から未登録テーブルを指定できない。
INSERT INTO import_target_catalog (
    table_name,
    display_name,
    description,
    tenant_scoped_flag,
    allow_delete_insert_flag,
    active_flag,
    created_at,
    updated_at,
    tenant_id
)
SELECT
    target.table_name,
    MIN(target.target_name),
    CONCAT('既存取込定義から移行: ', target.table_name),
    CASE
        WHEN target.table_name = 'resident_tax_monthly' THEN 1
        ELSE 0
    END,
    1,
    1,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6),
    target.tenant_id
FROM import_target target
WHERE target.deleted_at IS NULL
GROUP BY target.tenant_id, target.table_name;

INSERT INTO import_target_catalog_column (
    catalog_id,
    column_name,
    display_name,
    data_type,
    order_no,
    active_flag,
    created_at,
    updated_at,
    tenant_id
)
SELECT
    catalog.id,
    column_def.column_name,
    MIN(column_def.csv_header_name),
    MIN(column_def.data_type),
    MIN(column_def.order_no),
    1,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6),
    catalog.tenant_id
FROM import_target_catalog catalog
INNER JOIN import_target target
    ON target.tenant_id = catalog.tenant_id
   AND target.table_name = catalog.table_name
   AND target.deleted_at IS NULL
INNER JOIN import_column column_def
    ON column_def.target_id = target.id
   AND column_def.deleted_at IS NULL
GROUP BY
    catalog.id,
    catalog.tenant_id,
    column_def.column_name;
