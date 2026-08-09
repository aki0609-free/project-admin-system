-- V1給与計算で使用する法定控除マスター。
-- 既存の管理画面設定は上書きせず、不足コードだけを補完する。
-- 健康保険の給与計算値には介護保険を合算し、支援金は別項目として扱う。

INSERT INTO deduction_masters (
    deduction_code,
    deduction_name,
    deduction_type,
    calculation_type,
    rule_name,
    default_amount,
    allow_manual_input,
    min_amount,
    max_amount,
    deduction_unit,
    detail_view_type,
    show_on_daily_statement,
    show_on_monthly_statement,
    carry_to_monthly_settlement,
    display_order,
    enabled,
    note,
    tenant_id,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    seed.deduction_code,
    seed.deduction_name,
    'LEGAL',
    'AUTO',
    NULL,
    0,
    FALSE,
    0,
    10000000,
    seed.deduction_unit,
    seed.detail_view_type,
    FALSE,
    TRUE,
    FALSE,
    seed.display_order,
    TRUE,
    seed.note,
    'default',
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6),
    NULL
FROM (
    SELECT 'INCOME_TAX' AS deduction_code,
           '所得税' AS deduction_name,
           'PAYROLL' AS deduction_unit,
           'INCOME_TAX' AS detail_view_type,
           10 AS display_order,
           '給与所得税額表を参照して計算' AS note
    UNION ALL
    SELECT 'RESIDENT_TAX', '住民税', 'MONTHLY', 'RESIDENT_TAX', 20,
           '年度別の住民税月額を画面またはCSV取込で管理'
    UNION ALL
    SELECT 'HEALTH_INSURANCE', '健康・介護保険', 'MONTHLY', 'HEALTH_INSURANCE', 30,
           '健康保険料率と介護保険料率を対象者条件に応じて計算'
    UNION ALL
    SELECT 'CHILD_SUPPORT', '子ども・子育て支援金', 'MONTHLY', 'NONE', 40,
           '子ども・子育て支援金率を参照して計算'
    UNION ALL
    SELECT 'WELFARE_PENSION', '厚生年金', 'MONTHLY', 'PENSION', 50,
           '標準報酬月額と厚生年金保険料率を参照して計算'
    UNION ALL
    SELECT 'EMPLOYMENT_INSURANCE', '雇用保険', 'MONTHLY', 'EMPLOYMENT_INSURANCE', 60,
           '労働保険事業区分に対応する雇用保険料率を参照して計算'
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM deduction_masters existing
    WHERE existing.deduction_code = seed.deduction_code
);
