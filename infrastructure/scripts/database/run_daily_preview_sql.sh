#!/usr/bin/env bash
set -euo pipefail

if [[ "$#" -ne 5 ]]; then
  echo "Usage: $0 <secret-arn> <host> <port> <database> <sql-file>" >&2
  exit 1
fi

secret_arn="$1"
database_host="$2"
database_port="$3"
database_name="$4"
sql_file="$5"
work_dir="$(mktemp -d /tmp/project-admin-daily-preview.XXXXXX)"
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

mysql_command=(
  docker run --rm --interactive
  --network host
  --volume "${client_config}:/run/secrets/mysql-client.cnf:ro"
  --volume "${sql_file}:/work/daily-preview.sql:ro"
  mysql:8.4
  mysql
  --defaults-extra-file=/run/secrets/mysql-client.cnf
  "${database_name}"
)

"${mysql_command[@]}" < "${sql_file}"

invalid_output_type_count="$(
  "${mysql_command[@]}" \
    --batch \
    --skip-column-names \
    --execute "
      SELECT COUNT(*)
      FROM operation_report_preview
      WHERE tenant_id = 'default'
        AND report_code IN (
          'DAILY_LABOR_COST_PREVIEW',
          'DAILY_PAYMENT_PREPARATION'
        )
        AND output_type NOT IN ('HTML_PREVIEW', 'HTML_PRINT');
    "
)"
if [[ "${invalid_output_type_count}" != "0" ]]; then
  echo "Daily preview output_type validation failed." >&2
  exit 1
fi

"${mysql_command[@]}" --table --execute "
  SELECT table_name
  FROM information_schema.views
  WHERE table_schema = DATABASE()
    AND table_name IN (
      'vw_daily_labor_cost_preview',
      'vw_daily_payment_preparation_preview'
    )
  ORDER BY table_name;

  SELECT
    report_code,
    report_name,
    output_type,
    filter_column_name,
    html_template_version
  FROM operation_report_preview
  WHERE tenant_id = 'default'
    AND report_code IN (
      'DAILY_LABOR_COST_PREVIEW',
      'DAILY_PAYMENT_PREPARATION'
    )
  ORDER BY display_order;
"

echo "DAILY_PREVIEW_SQL_APPLIED"
