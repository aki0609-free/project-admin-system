-- ProjectAdminSystem V1
-- ローカルDocker専用：2026年9月の月次締め計算期間
--
-- 本番環境では適用しない。9月の日報・給与プロファイルを変更せず、
-- 月次給与計算の前提となる検証済み期間だけを再実行可能な形で用意する。

SET @fixture_tenant_id = 'default';
SET @fixture_target_month = '2026-09-01';

INSERT INTO payroll_calculation_period (
    target_month,
    income_tax_year,
    insurance_rate_year,
    child_care_support_required,
    rounding_mode,
    verified_flag,
    verified_at,
    verified_by,
    source_note,
    tenant_id,
    created_at,
    updated_at,
    deleted_at
) VALUES (
    @fixture_target_month,
    2026,
    2026,
    TRUE,
    'HALF_UP',
    TRUE,
    CURRENT_TIMESTAMP(6),
    'local-e2e',
    'ローカル2026年9月月次締め検証用。',
    @fixture_tenant_id,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6),
    NULL
)
ON DUPLICATE KEY UPDATE
    income_tax_year = IF(verified_by = 'local-e2e', VALUES(income_tax_year), income_tax_year),
    insurance_rate_year = IF(verified_by = 'local-e2e', VALUES(insurance_rate_year), insurance_rate_year),
    child_care_support_required = IF(verified_by = 'local-e2e', VALUES(child_care_support_required), child_care_support_required),
    rounding_mode = IF(verified_by = 'local-e2e', VALUES(rounding_mode), rounding_mode),
    verified_flag = IF(verified_by = 'local-e2e', TRUE, verified_flag),
    verified_at = IF(verified_by = 'local-e2e', CURRENT_TIMESTAMP(6), verified_at),
    source_note = IF(verified_by = 'local-e2e', VALUES(source_note), source_note),
    deleted_at = IF(verified_by = 'local-e2e', NULL, deleted_at),
    updated_at = CURRENT_TIMESTAMP(6);

-- 9月の日報対象者のうち社会保険計算対象で標準報酬月額が未登録の
-- ローカル検証従業員だけを補完する。本番データや登録済み履歴は上書きしない。
INSERT INTO employee_standard_remuneration (
    employee_id,
    effective_from,
    effective_to,
    health_standard_remuneration,
    pension_standard_remuneration,
    source_type,
    note,
    tenant_id,
    created_at,
    updated_at,
    deleted_at
)
SELECT DISTINCT
    employee.id,
    @fixture_target_month,
    NULL,
    300000,
    300000,
    'LOCAL_FIXTURE',
    '2026年9月Local月次締め検証用',
    @fixture_tenant_id,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6),
    NULL
FROM employee employee
INNER JOIN employee_payroll_profile profile
    ON profile.tenant_id = employee.tenant_id
   AND profile.employee_id = employee.id
   AND profile.deleted_at IS NULL
WHERE employee.tenant_id = @fixture_tenant_id
  AND employee.employee_code LIKE 'E2E-%'
  AND employee.deleted_at IS NULL
  AND (
      profile.social_insurance_flag = TRUE
      OR profile.health_insurance_flag = TRUE
      OR profile.pension_insurance_flag = TRUE
      OR profile.care_insurance_flag = TRUE
  )
  AND EXISTS (
      SELECT 1
      FROM daily_report report
      WHERE report.tenant_id = employee.tenant_id
        AND report.employee_id = employee.id
        AND report.work_date BETWEEN '2026-09-01' AND '2026-09-30'
        AND report.deleted_at IS NULL
  )
  AND NOT EXISTS (
      SELECT 1
      FROM employee_standard_remuneration remuneration
      WHERE remuneration.tenant_id = employee.tenant_id
        AND remuneration.employee_id = employee.id
        AND remuneration.effective_from <= '2026-09-30'
        AND (
            remuneration.effective_to IS NULL
            OR remuneration.effective_to >= '2026-09-01'
        )
        AND remuneration.deleted_at IS NULL
  )
ON DUPLICATE KEY UPDATE
    updated_at = VALUES(updated_at);
