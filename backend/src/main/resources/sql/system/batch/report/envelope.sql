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
    last_executed_at,
    next_execute_at,
    active_flag,
    description,
    created_at,
    updated_at
)
VALUES
(
    'default',
    'PRINT_ENVELOPE_NAGA3',
    '封筒印刷 長形3号',
    'REPORT',
    'ENVELOPE_NAGA3',
    TRUE,
    FALSE,
    'NONE',
    NULL,
    NULL,
    NULL,
    TRUE,
    '長形3号封筒の宛名印刷を実行します。',
    NOW(),
    NOW()
),
(
    'default',
    'PRINT_ENVELOPE_KAKU2',
    '封筒印刷 角形2号',
    'REPORT',
    'ENVELOPE_KAKU2',
    TRUE,
    FALSE,
    'NONE',
    NULL,
    NULL,
    NULL,
    TRUE,
    '角形2号封筒の宛名印刷を実行します。',
    NOW(),
    NOW()
);

UPDATE report_master
SET output_path = 'backend/reports/envelope'
WHERE report_code IN ('ENVELOPE_NAGA3', 'ENVELOPE_KAKU2');