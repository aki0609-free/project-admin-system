-- 令和9年分（2027年分）源泉徴収税額表の適用月設定。
--
-- 所得税は2027年1月支給分から2027年版を使用する。
-- 月払いは対象月の翌月15日支給のため、2026年12月対象分から適用する。
-- 社会保険料率は別の公式改定日を持つため、ここでは直近設定を仮引継ぎし、
-- verified_flagをFALSEにして月次締めの確定を防ぐ。

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
)
SELECT
    month_source.target_month,
    2027,
    COALESCE(previous.insurance_rate_year, 2026),
    COALESCE(previous.child_care_support_required, FALSE),
    COALESCE(previous.rounding_mode, 'HALF_UP'),
    FALSE,
    NULL,
    NULL,
    '令和9年分源泉徴収税額表を適用。社会保険料率は対象月ごとに確認後、検証済みへ変更すること。',
    tenant_source.tenant_id,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6),
    NULL
FROM (
    SELECT DATE('2026-12-01') AS target_month UNION ALL
    SELECT DATE('2027-01-01') UNION ALL
    SELECT DATE('2027-02-01') UNION ALL
    SELECT DATE('2027-03-01') UNION ALL
    SELECT DATE('2027-04-01') UNION ALL
    SELECT DATE('2027-05-01') UNION ALL
    SELECT DATE('2027-06-01') UNION ALL
    SELECT DATE('2027-07-01') UNION ALL
    SELECT DATE('2027-08-01') UNION ALL
    SELECT DATE('2027-09-01') UNION ALL
    SELECT DATE('2027-10-01') UNION ALL
    SELECT DATE('2027-11-01')
) month_source
CROSS JOIN (
    SELECT DISTINCT tenant_id
    FROM payroll_calculation_period
    WHERE deleted_at IS NULL
    UNION
    SELECT 'default'
) tenant_source
LEFT JOIN payroll_calculation_period previous
  ON previous.id = (
      SELECT prior.id
      FROM payroll_calculation_period prior
      WHERE prior.tenant_id = tenant_source.tenant_id
        AND prior.target_month < DATE('2026-12-01')
        AND prior.deleted_at IS NULL
      ORDER BY prior.target_month DESC, prior.id DESC
      LIMIT 1
  )
WHERE TRUE
ON DUPLICATE KEY UPDATE
    income_tax_year = 2027,
    verified_flag = CASE
        WHEN payroll_calculation_period.income_tax_year = 2027
            THEN payroll_calculation_period.verified_flag
        ELSE FALSE
    END,
    verified_at = CASE
        WHEN payroll_calculation_period.income_tax_year = 2027
            THEN payroll_calculation_period.verified_at
        ELSE NULL
    END,
    verified_by = CASE
        WHEN payroll_calculation_period.income_tax_year = 2027
            THEN payroll_calculation_period.verified_by
        ELSE NULL
    END,
    source_note = CASE
        WHEN payroll_calculation_period.income_tax_year = 2027
            THEN payroll_calculation_period.source_note
        ELSE VALUES(source_note)
    END,
    updated_at = CURRENT_TIMESTAMP(6),
    deleted_at = NULL;
