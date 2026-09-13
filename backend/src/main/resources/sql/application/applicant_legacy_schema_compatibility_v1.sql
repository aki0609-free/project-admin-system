-- 応募者の旧動的項目列は現行エンティティでは使用しない。
-- 旧環境に列が残っていても現行APIから応募者を登録できるよう、値を保持したままNULL許可へ変更する。
SET @applicant_dynamic_fields_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'applicants'
      AND column_name = 'dynamic_fields'
);

SET @applicant_dynamic_fields_sql := IF(
    @applicant_dynamic_fields_exists > 0,
    'ALTER TABLE applicants MODIFY COLUMN dynamic_fields JSON NULL',
    'SELECT 1'
);

PREPARE applicant_dynamic_fields_statement FROM @applicant_dynamic_fields_sql;
EXECUTE applicant_dynamic_fields_statement;
DEALLOCATE PREPARE applicant_dynamic_fields_statement;
