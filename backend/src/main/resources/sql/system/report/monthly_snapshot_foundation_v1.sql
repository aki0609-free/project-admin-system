-- 帳票マスタ 月次スナップショット・S3 HTMLテンプレート拡張
-- MySQL 8.x / 1回だけ実行する。

SET NAMES utf8mb4;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE report_master ADD COLUMN source_view_name VARCHAR(200) NULL AFTER output_table',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'report_master'
      AND column_name = 'source_view_name'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE report_master ADD COLUMN history_table VARCHAR(200) NULL AFTER source_view_name',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'report_master'
      AND column_name = 'history_table'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE report_master ADD COLUMN html_template_key VARCHAR(1000) NULL AFTER history_table',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'report_master'
      AND column_name = 'html_template_key'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE report_master ADD COLUMN html_template_version INT NULL AFTER html_template_key',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'report_master'
      AND column_name = 'html_template_version'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE report_master ADD COLUMN html_template_hash VARCHAR(128) NULL AFTER html_template_version',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'report_master'
      AND column_name = 'html_template_hash'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 月次帳票のinputTableには次の実行制御カラムを持たせる。
-- 実テーブルごとのALTERは各帳票SQLで実施する。
--
-- execution_id      VARCHAR(100) NOT NULL
-- tenant_id         VARCHAR(255) NOT NULL
-- target_month      DATE NOT NULL
-- closing_version   INT NOT NULL
-- execution_mode    VARCHAR(30) NOT NULL
--
-- execution_mode:
-- INITIAL : 初回締め。最新ViewからhistoryTableへ保存
-- RECLOSE : 再締め。新Versionとして最新Viewから保存
-- RETRY   : 同じVersionのhistoryTableからoutputTableを再作成

-- ストアドの共通契約：
--
-- CREATE PROCEDURE sp_xxx_monthly_snapshot(IN p_execution_id VARCHAR(100))
-- BEGIN
--   1. inputTableからtenant_id、target_month、
--      closing_version、execution_modeを取得
--   2. INITIAL / RECLOSE:
--        View -> historyTable
--   3. RETRY:
--        historyTableは変更しない
--   4. 全モード:
--        historyTable -> outputTable
-- END;
--
-- historyTableの推奨一意制約：
-- UNIQUE (
--   tenant_id,
--   target_month,
--   closing_version,
--   business_key
-- )
--
-- outputTableはexecution_idで分離する。
