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

-- 入金確認表：同じ請求対象月に対する当月・翌月・翌々月入金の確認用。
-- 台帳の対象月は請求対象月であり、入金予定月ではない。
DROP TEMPORARY TABLE IF EXISTS tmp_receipt_confirmation_fixture;
CREATE TEMPORARY TABLE tmp_receipt_confirmation_fixture (
    customer_name varchar(255) primary key,
    payment_month_offset int not null,
    payment_day_value int not null,
    billing_amount int not null,
    expected_payment_date date not null,
    paid_amount int not null,
    fee int not null,
    payment_status varchar(30) not null,
    note varchar(255) not null
);

INSERT INTO tmp_receipt_confirmation_fixture VALUES
    ('E2E 建設株式会社', 0, 25, 321000, DATE '2026-09-25', 321000, 0,
     'PAID', '入金確認表テスト：当月入金'),
    ('E2E 土木株式会社', 1, 25, 452550, DATE '2026-10-25', 452000, 550,
     'PAID', '入金確認表テスト：翌月入金'),
    ('E2E 設備株式会社', 2, 25, 583000, DATE '2026-11-25', 300000, 0,
     'PARTIAL', '入金確認表テスト：翌々月・一部入金');

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
    customer.id, '2026-09',
    customer.closing_day_type, customer.closing_day_value,
    COALESCE(customer.closing_month_offset, 0),
    'DAY_OF_MONTH', fixture.payment_day_value,
    fixture.payment_month_offset,
    fixture.billing_amount, fixture.expected_payment_date,
    CASE
        WHEN fixture.paid_amount > 0 THEN fixture.expected_payment_date
        ELSE NULL
    END,
    fixture.paid_amount, fixture.fee, 0, 0,
    fixture.paid_amount + fixture.fee,
    fixture.payment_status, fixture.note, 'LOCAL_RECEIPT_FIXTURE'
FROM tmp_receipt_confirmation_fixture fixture
JOIN customers customer
  ON customer.tenant_id = @fixture_tenant_id
 AND customer.name = fixture.customer_name
 AND customer.deleted_at IS NULL
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

DROP TEMPORARY TABLE IF EXISTS tmp_receipt_confirmation_fixture;
