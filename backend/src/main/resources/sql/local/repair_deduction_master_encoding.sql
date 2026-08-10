-- ProjectAdminSystem V1
-- ローカルDocker専用：過去に文字コード未指定で投入された法定控除名の補正
--
-- 本番用Runtime Schemaには含めない。
-- 控除コードで対象を限定し、業務設定・金額・Rule設定は変更しない。

UPDATE deduction_masters
SET deduction_name = CASE deduction_code
        WHEN 'INCOME_TAX' THEN '所得税'
        WHEN 'RESIDENT_TAX' THEN '住民税'
        WHEN 'HEALTH_INSURANCE' THEN '健康・介護保険'
        WHEN 'CHILD_SUPPORT' THEN '子ども・子育て支援金'
        WHEN 'WELFARE_PENSION' THEN '厚生年金'
        WHEN 'EMPLOYMENT_INSURANCE' THEN '雇用保険'
        ELSE deduction_name
    END,
    note = CASE deduction_code
        WHEN 'INCOME_TAX' THEN '給与所得税額表を参照して計算'
        WHEN 'RESIDENT_TAX' THEN '年度別の住民税月額を画面またはCSV取込で管理'
        WHEN 'HEALTH_INSURANCE' THEN '健康保険料率と介護保険料率を対象者条件に応じて計算'
        WHEN 'CHILD_SUPPORT' THEN '子ども・子育て支援金率を参照して計算'
        WHEN 'WELFARE_PENSION' THEN '標準報酬月額と厚生年金保険料率を参照して計算'
        WHEN 'EMPLOYMENT_INSURANCE' THEN '労働保険事業区分に対応する雇用保険料率を参照して計算'
        ELSE note
    END,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE tenant_id = 'default'
  AND deduction_code IN (
      'INCOME_TAX',
      'RESIDENT_TAX',
      'HEALTH_INSURANCE',
      'CHILD_SUPPORT',
      'WELFARE_PENSION',
      'EMPLOYMENT_INSURANCE'
  )
  AND deleted_at IS NULL;
