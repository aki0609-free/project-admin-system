-- =========================================================
-- batch_job_definition
-- =========================================================

INSERT INTO batch_job_definition (
    tenant_id,
    job_code,
    job_name,
    job_type,
    target_code,
    immediate_executable,
    schedule_enabled,
    schedule_type,
    cron_expression,
    active_flag,
    description,
    created_at,
    updated_at
) VALUES

-- Backup
(
    'default',
    'BACKUP_USERS_MASTER',
    'ユーザーマスタバックアップ',
    'BACKUP',
    'users_master',
    true,
    true,
    'CRON',
    '0 0 1 * * *',
    true,
    'usersテーブルを毎日1時にバックアップ',
    NOW(),
    NOW()
),

(
    'default',
    'BACKUP_NOTICE',
    'お知らせバックアップ',
    'BACKUP',
    'notice_master',
    true,
    false,
    'NONE',
    NULL,
    true,
    'notice関連バックアップ',
    NOW(),
    NOW()
),

-- Import
(
    'default',
    'IMPORT_EMPLOYEE',
    '社員CSV取込',
    'IMPORT',
    'employee_import',
    true,
    true,
    'CRON',
    '0 30 2 * * *',
    true,
    '社員CSVを毎日2:30に取込',
    NOW(),
    NOW()
),

(
    'default',
    'IMPORT_ATTENDANCE',
    '勤怠CSV取込',
    'IMPORT',
    'attendance_import',
    true,
    true,
    'CRON',
    '0 */15 * * * *',
    true,
    '勤怠CSVを15分毎に取込',
    NOW(),
    NOW()
),

-- Mail
(
    'default',
    'MAIL_WAITING_SEND',
    '待機メール送信',
    'MAIL',
    'waiting_mail',
    true,
    true,
    'CRON',
    '0 */5 * * * *',
    true,
    'WAITINGメールを5分毎に送信',
    NOW(),
    NOW()
),

-- Report
(
    'default',
    'REPORT_MONTHLY_SALES',
    '月次売上帳票',
    'REPORT',
    'monthly_sales_report',
    true,
    true,
    'CRON',
    '0 0 8 1 * *',
    true,
    '毎月1日8時に月次売上帳票生成',
    NOW(),
    NOW()
),

(
    'default',
    'REPORT_DAILY_ATTENDANCE',
    '日次勤怠帳票',
    'REPORT',
    'daily_attendance_report',
    true,
    false,
    'NONE',
    NULL,
    true,
    '日次勤怠帳票',
    NOW(),
    NOW()
),

-- Report + Mail
(
    'default',
    'REPORT_MAIL_MONTHLY',
    '月次帳票メール送信',
    'REPORT_MAIL',
    'monthly_report_mail',
    true,
    true,
    'CRON',
    '0 0 9 1 * *',
    true,
    '帳票生成後メール送信',
    NOW(),
    NOW()
);



-- =========================================================
-- batch_execution_log
-- =========================================================

INSERT INTO batch_execution_log (
    tenant_id,
    job_code,
    job_name,
    job_type,
    target_code,
    status,
    started_at,
    finished_at,
    message,
    error_message,
    created_at,
    updated_at
) VALUES
(
    'default',
    'BACKUP_USERS_MASTER',
    'ユーザーマスタバックアップ',
    'BACKUP',
    'users_master',
    'COMPLETED',
    DATE_SUB(NOW(), INTERVAL 2 HOUR),
    DATE_ADD(DATE_SUB(NOW(), INTERVAL 2 HOUR), INTERVAL 20 SECOND),
    'バックアップ完了',
    NULL,
    NOW(),
    NOW()
),
(
    'default',
    'IMPORT_EMPLOYEE',
    '社員CSV取込',
    'IMPORT',
    'employee_import',
    'COMPLETED',
    DATE_SUB(NOW(), INTERVAL 90 MINUTE),
    DATE_ADD(DATE_SUB(NOW(), INTERVAL 90 MINUTE), INTERVAL 1 MINUTE),
    'CSV取込完了',
    NULL,
    NOW(),
    NOW()
),
(
    'default',
    'IMPORT_ATTENDANCE',
    '勤怠CSV取込',
    'IMPORT',
    'attendance_import',
    'FAILED',
    DATE_SUB(NOW(), INTERVAL 50 MINUTE),
    DATE_ADD(DATE_SUB(NOW(), INTERVAL 50 MINUTE), INTERVAL 1 MINUTE),
    'CSV取込失敗',
    'Duplicate entry detected.',
    NOW(),
    NOW()
),
(
    'default',
    'MAIL_WAITING_SEND',
    '待機メール送信',
    'MAIL',
    'waiting_mail',
    'COMPLETED',
    DATE_SUB(NOW(), INTERVAL 30 MINUTE),
    DATE_ADD(DATE_SUB(NOW(), INTERVAL 30 MINUTE), INTERVAL 1 MINUTE),
    'メール送信完了 sent=15 failed=0',
    NULL,
    NOW(),
    NOW()
),
(
    'default',
    'REPORT_MONTHLY_SALES',
    '月次売上帳票',
    'REPORT',
    'monthly_sales_report',
    'COMPLETED',
    DATE_SUB(NOW(), INTERVAL 1 DAY),
    DATE_ADD(DATE_SUB(NOW(), INTERVAL 1 DAY), INTERVAL 45 SECOND),
    '帳票生成完了',
    NULL,
    NOW(),
    NOW()
),
(
    'default',
    'REPORT_MAIL_MONTHLY',
    '月次帳票メール送信',
    'REPORT_MAIL',
    'monthly_report_mail',
    'FAILED',
    DATE_SUB(NOW(), INTERVAL 10 MINUTE),
    DATE_ADD(DATE_SUB(NOW(), INTERVAL 10 MINUTE), INTERVAL 1 MINUTE),
    'メール送信失敗',
    'SMTP Authentication failed.',
    NOW(),
    NOW()
);