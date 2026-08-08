#!/usr/bin/env bash
set -euo pipefail

if [[ "$#" -lt 5 ]]; then
  echo "Usage: $0 <secret-arn> <host> <port> <database> <sql-file>..." >&2
  exit 1
fi

secret_arn="$1"
database_host="$2"
database_port="$3"
database_name="$4"
shift 4
sql_files=("$@")

work_dir="$(mktemp -d /tmp/project-admin-runtime-schema.XXXXXX)"
client_config="${work_dir}/mysql-client.cnf"

cleanup() {
  rm -rf "${work_dir}"
}
trap cleanup EXIT

secret_json=""
for attempt in {1..6}; do
  if secret_json="$(
    aws secretsmanager get-secret-value \
      --secret-id "${secret_arn}" \
      --query SecretString \
      --output text
  )"; then
    break
  fi

  if [[ "${attempt}" -eq 6 ]]; then
    echo "Failed to read the RDS secret after ${attempt} attempts." >&2
    exit 1
  fi
  echo "Waiting for the temporary Secrets Manager permission to propagate (attempt ${attempt}/6)..." >&2
  sleep 10
done

SECRET_JSON="${secret_json}" \
DB_HOST="${database_host}" \
DB_PORT="${database_port}" \
CLIENT_CONFIG="${client_config}" \
  python3 -c '
import json
import os

secret = json.loads(os.environ["SECRET_JSON"])

def quote(value):
    text = str(value)
    escaped = text.replace("\\", "\\\\").replace("\"", "\\\"")
    return f"\"{escaped}\""

options = {
    "user": secret["username"],
    "password": secret["password"],
    "host": secret.get("host", os.environ["DB_HOST"]),
    "port": secret.get("port", os.environ["DB_PORT"]),
    "ssl-mode": "REQUIRED",
    "default-character-set": "utf8mb4",
}
with open(os.environ["CLIENT_CONFIG"], "w", encoding="utf-8") as file:
    file.write("[client]\n")
    for key, value in options.items():
        file.write(f"{key}={quote(value)}\n")
'
unset secret_json
chmod 0600 "${client_config}"

docker pull mysql:8.4 >/dev/null

run_mysql() {
  local sql_file="${1:-}"
  shift || true
  local command=(
    docker run --rm --interactive
    --network host
    --volume "${client_config}:/run/secrets/mysql-client.cnf:ro"
  )
  if [[ -n "${sql_file}" ]]; then
    command+=(--volume "${sql_file}:/work/upgrade.sql:ro")
  fi
  command+=(
    mysql:8.4 mysql
    --defaults-extra-file=/run/secrets/mysql-client.cnf
    "${database_name}"
    "$@"
  )

  if [[ -n "${sql_file}" ]]; then
    "${command[@]}" < "${sql_file}"
  else
    "${command[@]}"
  fi
}

for sql_file in "${sql_files[@]}"; do
  echo "Applying $(basename "${sql_file}")..."
  run_mysql "${sql_file}"
done

verification="$(
  run_mysql "" --batch --skip-column-names --execute "
    SELECT CONCAT(
      (SELECT COUNT(*) FROM information_schema.columns
       WHERE table_schema = DATABASE()
         AND table_name = 'excel_book_master'
         AND column_name IN (
           'layout_type', 'renderer_key', 'selection_mode',
           'selection_source_name', 'selection_value_column',
           'selection_display_columns', 'allow_select_all',
           'generation_unit', 'print_paper_size',
           'print_orientation', 'print_fit_to_one_page'
         )),
      ':',
      (SELECT COUNT(*) FROM information_schema.columns
       WHERE table_schema = DATABASE()
         AND table_name = 'report_master'
         AND column_name IN (
           'source_view_name', 'history_table', 'html_template_key',
           'html_template_version', 'html_template_hash'
         )),
      ':',
      (SELECT COUNT(*) FROM information_schema.tables
       WHERE table_schema = DATABASE()
         AND table_name IN (
           'monthly_closing_execution',
           'monthly_closing_output_definition',
           'monthly_closing_item'
         )),
      ':',
      (SELECT COUNT(*) FROM information_schema.columns
       WHERE table_schema = DATABASE()
         AND table_name = 'daily_report'
         AND column_name IN (
           'normal_pay_amount', 'overtime_pay_amount',
           'night_pay_amount', 'holiday_pay_amount'
         )),
      ':',
      (SELECT COUNT(*) FROM information_schema.tables
       WHERE table_schema = DATABASE()
         AND table_name = 'daily_pay_rule_setting')
    );
  "
)"

if [[ "${verification}" != "11:5:3:4:1" ]]; then
  echo "Runtime schema verification failed: ${verification}" >&2
  exit 1
fi

daily_preview_verification="$(
  run_mysql "" --batch --skip-column-names --execute "
    SELECT CONCAT(
      (SELECT COUNT(*)
       FROM information_schema.views
       WHERE table_schema = DATABASE()
         AND table_name IN (
           'vw_daily_labor_cost_preview',
           'vw_daily_payment_preparation_preview'
         )),
      ':',
      (SELECT COUNT(DISTINCT report_code)
       FROM operation_report_preview
       WHERE tenant_id = 'default'
         AND active_flag = TRUE
         AND deleted_at IS NULL
         AND report_code IN (
           'DAILY_LABOR_COST_PREVIEW',
           'DAILY_PAYMENT_PREPARATION'
         ))
    );
  "
)"

if [[ "${daily_preview_verification}" != "2:2" ]]; then
  echo "Daily preview verification failed: ${daily_preview_verification}" >&2
  exit 1
fi

daily_pay_slip_verification="$(
  run_mysql "" --batch --skip-column-names --execute "
    SELECT CONCAT(
      (SELECT is_nullable
       FROM information_schema.columns
       WHERE table_schema = DATABASE()
         AND table_name = 'daily_pay_slip_input'
         AND column_name = 'employee_id'),
      ':',
      (SELECT COUNT(*)
       FROM information_schema.views
       WHERE table_schema = DATABASE()
         AND table_name = 'vw_daily_pay_slip_latest'),
      ':',
      (SELECT COUNT(*)
       FROM information_schema.routines
       WHERE routine_schema = DATABASE()
         AND routine_type = 'PROCEDURE'
         AND routine_name = 'sp_daily_pay_slip_prepare'),
      ':',
      (SELECT COUNT(*)
       FROM report_master
       WHERE tenant_id = 'default'
         AND report_code = 'DAILY_PAY_SLIP'
         AND active_flag = TRUE
         AND deleted_at IS NULL),
      ':',
      (SELECT COUNT(*)
       FROM batch_job_definition
       WHERE tenant_id = 'default'
         AND job_code = 'PRINT_DAILY_PAY_SLIP'
         AND active_flag = TRUE
         AND deleted_at IS NULL),
      ':',
      (SELECT COUNT(*)
       FROM operation_report_preview
       WHERE tenant_id = 'default'
         AND report_code = 'DAILY_PAY_SLIP'
         AND html_template_key = 'documents/templates/reports/html/DAILY_PAY_SLIP/v2/template.html'
         AND html_template_version = 2
         AND active_flag = TRUE
         AND deleted_at IS NULL)
    );
  "
)"

if [[ "${daily_pay_slip_verification}" != "YES:1:1:1:1:1" ]]; then
  echo "Daily pay slip verification failed: ${daily_pay_slip_verification}" >&2
  exit 1
fi

customer_transaction_sync_verification="$(
  run_mysql "" --batch --skip-column-names --execute "
    SELECT CONCAT(
      (SELECT COUNT(*)
       FROM information_schema.columns
       WHERE table_schema = DATABASE()
         AND table_name = 'customer_transactions'
         AND column_name IN (
           'source_type',
           'source_invoice_history_id',
           'source_closing_version'
         )),
      ':',
      (SELECT COUNT(*)
       FROM information_schema.statistics
       WHERE table_schema = DATABASE()
         AND table_name = 'customer_transactions'
         AND index_name = 'uk_customer_transaction_month')
    );
  "
)"

if [[ "${customer_transaction_sync_verification}" != "3:3" ]]; then
  echo "Customer transaction sync verification failed: ${customer_transaction_sync_verification}" >&2
  exit 1
fi

business_settings_verification="$(
  run_mysql "" --batch --skip-column-names --execute "
    SELECT CONCAT(
      (SELECT COUNT(*)
       FROM information_schema.tables
       WHERE table_schema = DATABASE()
         AND table_name = 'employee_resignation_setting'),
      ':',
      (SELECT COUNT(*)
       FROM information_schema.statistics
       WHERE table_schema = DATABASE()
         AND table_name = 'employee_resignation_checklist_master'
         AND index_name = 'uk_employee_resignation_checklist_code')
    );
  "
)"

if [[ "${business_settings_verification}" != "1:2" ]]; then
  echo "Business settings verification failed: ${business_settings_verification}" >&2
  exit 1
fi

run_mysql "" --table --execute "
  SELECT table_name, column_name, column_type
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND (
      (table_name = 'excel_book_master' AND column_name IN (
        'layout_type', 'renderer_key', 'selection_mode',
        'selection_source_name', 'selection_value_column',
        'selection_display_columns', 'allow_select_all',
        'generation_unit', 'print_paper_size',
        'print_orientation', 'print_fit_to_one_page'
      ))
      OR
      (table_name = 'report_master' AND column_name IN (
        'source_view_name', 'history_table', 'html_template_key',
        'html_template_version', 'html_template_hash'
      ))
    )
  ORDER BY table_name, ordinal_position;

  SELECT table_name
  FROM information_schema.tables
  WHERE table_schema = DATABASE()
    AND table_name IN (
      'monthly_closing_execution',
      'monthly_closing_output_definition',
      'monthly_closing_item'
    )
  ORDER BY table_name;
"

echo "RUNTIME_SCHEMA_UPGRADE_APPLIED"
