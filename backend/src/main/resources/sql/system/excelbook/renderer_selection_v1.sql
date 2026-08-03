-- ProjectAdminSystem V1
-- Spreadsheet Renderer Registry／汎用選択・印刷設定
-- 既存行は従来動作を維持する既定値で移行する。

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE excel_book_master ADD COLUMN renderer_key VARCHAR(100) NULL AFTER layout_type',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'excel_book_master'
      AND column_name = 'renderer_key'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE excel_book_master
SET renderer_key = layout_type
WHERE renderer_key IS NULL OR renderer_key = '';

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE excel_book_master ADD COLUMN selection_mode VARCHAR(20) NOT NULL DEFAULT ''NONE'' AFTER renderer_key',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'excel_book_master'
      AND column_name = 'selection_mode'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE excel_book_master ADD COLUMN selection_source_name VARCHAR(100) NULL AFTER selection_mode',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'excel_book_master'
      AND column_name = 'selection_source_name'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE excel_book_master ADD COLUMN selection_value_column VARCHAR(100) NULL AFTER selection_source_name',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'excel_book_master'
      AND column_name = 'selection_value_column'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE excel_book_master ADD COLUMN selection_display_columns VARCHAR(1000) NULL AFTER selection_value_column',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'excel_book_master'
      AND column_name = 'selection_display_columns'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE excel_book_master ADD COLUMN allow_select_all TINYINT(1) NOT NULL DEFAULT 0 AFTER selection_display_columns',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'excel_book_master'
      AND column_name = 'allow_select_all'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE excel_book_master ADD COLUMN generation_unit VARCHAR(30) NOT NULL DEFAULT ''ONE_FILE'' AFTER allow_select_all',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'excel_book_master'
      AND column_name = 'generation_unit'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE excel_book_master ADD COLUMN print_paper_size VARCHAR(20) NOT NULL DEFAULT ''A4'' AFTER generation_unit',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'excel_book_master'
      AND column_name = 'print_paper_size'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE excel_book_master ADD COLUMN print_orientation VARCHAR(20) NOT NULL DEFAULT ''PORTRAIT'' AFTER print_paper_size',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'excel_book_master'
      AND column_name = 'print_orientation'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE excel_book_master ADD COLUMN print_fit_to_one_page TINYINT(1) NOT NULL DEFAULT 0 AFTER print_orientation',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'excel_book_master'
      AND column_name = 'print_fit_to_one_page'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
