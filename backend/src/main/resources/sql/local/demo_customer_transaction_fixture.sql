-- ProjectAdminSystem V1
-- ローカルDocker専用：取引管理の表示・入金判定確認fixture
-- 本番環境では適用しない。顧客・対象月単位で再実行可能。

SET @fixture_tenant_id = 'default';
SET @fixture_customer_id = (
    SELECT MIN(id)
    FROM customers
    WHERE tenant_id = @fixture_tenant_id
      AND name = 'E2E 月間集計検証顧客'
      AND deleted_at IS NULL
);

INSERT INTO customer_transactions (
    tenant_id, created_at, updated_at, deleted_at,
    customer_id, target_month,
    closing_day_type, closing_day_value, closing_month_offset,
    payment_day_type, payment_day_value, payment_month_offset,
    billing_amount, expected_payment_date, confirmed_payment_date,
    paid_amount, fee, offset_amount, adjustment_amount,
    total_amount, payment_status, note, source_type
)
SELECT
    @fixture_tenant_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL,
    @fixture_customer_id, fixture.target_month,
    'END_OF_MONTH', NULL, 0,
    'DAY_OF_MONTH', 25, 1,
    fixture.billing_amount, fixture.expected_payment_date,
    fixture.confirmed_payment_date,
    fixture.paid_amount, fixture.fee, fixture.offset_amount,
    fixture.adjustment_amount, fixture.total_amount,
    fixture.payment_status, fixture.note, 'LOCAL_FIXTURE'
FROM (
    SELECT '2026-05' AS target_month, 250000 AS billing_amount,
           DATE '2026-06-25' AS expected_payment_date, NULL AS confirmed_payment_date,
           0 AS paid_amount, 0 AS fee, 0 AS offset_amount, 0 AS adjustment_amount,
           0 AS total_amount, 'UNPAID' AS payment_status,
           'ローカル確認用：未入金' AS note
    UNION ALL
    SELECT '2026-06', 330000, DATE '2026-07-25', DATE '2026-07-20',
           200000, 550, 0, 0, 200550, 'PARTIAL',
           'ローカル確認用：一部入金'
    UNION ALL
    SELECT '2026-07', 480000, DATE '2026-08-25', DATE '2026-08-20',
           479450, 550, 0, 0, 480000, 'PAID',
           'ローカル確認用：入金済'
    UNION ALL
    SELECT '2026-08', 120003, DATE '2026-09-25', DATE '2026-09-25',
           120000, 0, 0, 3, 120003, 'PAID',
           'ローカル確認用：その他調整額3円を含む'
) fixture
WHERE @fixture_customer_id IS NOT NULL
ON DUPLICATE KEY UPDATE
    closing_day_type = VALUES(closing_day_type),
    closing_day_value = VALUES(closing_day_value),
    closing_month_offset = VALUES(closing_month_offset),
    payment_day_type = VALUES(payment_day_type),
    payment_day_value = VALUES(payment_day_value),
    payment_month_offset = VALUES(payment_month_offset),
    billing_amount = VALUES(billing_amount),
    expected_payment_date = VALUES(expected_payment_date),
    confirmed_payment_date = VALUES(confirmed_payment_date),
    paid_amount = VALUES(paid_amount),
    fee = VALUES(fee),
    offset_amount = VALUES(offset_amount),
    adjustment_amount = VALUES(adjustment_amount),
    total_amount = VALUES(total_amount),
    payment_status = VALUES(payment_status),
    note = VALUES(note),
    source_type = VALUES(source_type),
    deleted_at = NULL,
    updated_at = CURRENT_TIMESTAMP(6);
