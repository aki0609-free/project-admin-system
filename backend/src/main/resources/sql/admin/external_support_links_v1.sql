-- ヘッダーから開くJira Forms・ConfluenceマニュアルのURL設定。
-- 管理者が画面から変更でき、ログイン済みユーザーが共通参照する。

CREATE TABLE IF NOT EXISTS external_support_link_setting (
    id BIGINT NOT NULL AUTO_INCREMENT,
    setting_code VARCHAR(50) NOT NULL,
    incident_report_url VARCHAR(2048) NOT NULL,
    manual_url VARCHAR(2048) NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_external_support_link_setting_code
        UNIQUE (tenant_id, setting_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO external_support_link_setting (
    setting_code,
    incident_report_url,
    manual_url,
    tenant_id,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    'DEFAULT',
    CONCAT(
        'https://projectadmin1215.atlassian.net/jira/software/projects/FUYO/form/1',
        '?atlOrigin=eyJpIjoiYTFkN2E2NWU2YWYwNGQ2ODk4MDRmMTliM2JkMjQ3YjgiLCJwIjoiaiJ9'
    ),
    CONCAT(
        'https://projectadmin1215.atlassian.net/wiki/spaces/',
        '~712020d0db24f25d734730b24dfb1508d24613/folder/19464193'
    ),
    'default',
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6),
    NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM external_support_link_setting
    WHERE tenant_id = 'default'
      AND setting_code = 'DEFAULT'
      AND deleted_at IS NULL
);
