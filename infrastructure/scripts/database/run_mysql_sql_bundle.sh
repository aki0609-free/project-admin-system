#!/usr/bin/env bash
set -euo pipefail

if [[ "$#" -lt 6 ]]; then
  echo "Usage: $0 <secret-arn> <host> <port> <database> <ddl-file> <seed-file>" >&2
  exit 1
fi

secret_arn="$1"
database_host="$2"
database_port="$3"
database_name="$4"
ddl_file="$5"
seed_file="$6"
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
client_config="${script_dir}/mysql-client.cnf"

cleanup() {
  rm -f "${client_config}"
}
trap cleanup EXIT

secret_json=""
for attempt in {1..12}; do
  secret_json="$(
    aws secretsmanager get-secret-value \
      --secret-id "${secret_arn}" \
      --query SecretString \
      --output text 2>/dev/null || true
  )"
  if [[ -n "${secret_json}" ]]; then
    break
  fi
  sleep 5
done

if [[ -z "${secret_json}" ]]; then
  echo "RDS master secret could not be read." >&2
  exit 1
fi

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

mysql_image="mysql:8.4"
docker pull "${mysql_image}" >/dev/null

mysql_command=(
  docker run --rm --interactive
  --network host
  --volume "${client_config}:/run/secrets/mysql-client.cnf:ro"
  --volume "${ddl_file}:/work/ddl.sql:ro"
  --volume "${seed_file}:/work/seed.sql:ro"
  "${mysql_image}"
  mysql
  --defaults-extra-file=/run/secrets/mysql-client.cnf
  "${database_name}"
)

existing_count="$(
  "${mysql_command[@]}" --batch --skip-column-names --execute "
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name IN (
        'excel_book_data_source_catalog',
        'excel_book_data_source_catalog_column',
        'excel_book_variable_mapping'
      );
  "
)"

if [[ "${existing_count}" != "0" ]]; then
  echo "Spreadsheet ledger tables already exist; refusing duplicate DDL application." >&2
  exit 1
fi

"${mysql_command[@]}" < "${ddl_file}"
"${mysql_command[@]}" < "${seed_file}"

"${mysql_command[@]}" --table --execute "
  SELECT table_name
  FROM information_schema.tables
  WHERE table_schema = DATABASE()
    AND table_name IN (
      'excel_book_data_source_catalog',
      'excel_book_data_source_catalog_column',
      'excel_book_variable_mapping'
    )
  ORDER BY table_name;

  SELECT source_code, display_name, physical_name, active_flag
  FROM excel_book_data_source_catalog
  WHERE tenant_id = 'default'
  ORDER BY source_code;

  SELECT COUNT(*) AS catalog_column_count
  FROM excel_book_data_source_catalog_column;
"

echo "MYSQL_SPREADSHEET_LEDGER_DDL_APPLIED"
