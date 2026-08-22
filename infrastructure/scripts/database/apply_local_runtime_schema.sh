#!/bin/sh

set -eu

manifest="/resources/sql/runtime-schema-manifest.txt"

if [ ! -f "${manifest}" ]; then
  echo "Runtime schema manifest was not found: ${manifest}" >&2
  exit 1
fi

while IFS= read -r resource || [ -n "${resource}" ]; do
  case "${resource}" in
    ""|\#*) continue ;;
  esac

  sql_file="/resources/${resource}"
  if [ ! -f "${sql_file}" ]; then
    echo "Runtime schema asset was not found: ${sql_file}" >&2
    exit 1
  fi

  echo "Applying runtime schema asset: ${resource}"
  mysql \
    --protocol=tcp \
    --host=mysql \
    --user="${MYSQL_USER}" \
    --default-character-set=utf8mb4 \
    "${MYSQL_DATABASE}" < "${sql_file}"
done < "${manifest}"

for local_resource in \
  "sql/local/repair_deduction_master_encoding.sql" \
  "sql/local/demo_monthly_summary_fixture.sql" \
  "sql/local/demo_monthly_payroll_fixture.sql" \
  "sql/local/demo_customer_transaction_fixture.sql"
do
  local_fixture="/resources/${local_resource}"
  if [ ! -f "${local_fixture}" ]; then
    echo "Local runtime asset was not found: ${local_fixture}" >&2
    exit 1
  fi

  echo "Applying local runtime asset: ${local_resource}"
  mysql \
    --protocol=tcp \
    --host=mysql \
    --user="${MYSQL_USER}" \
    --default-character-set=utf8mb4 \
    "${MYSQL_DATABASE}" < "${local_fixture}"
done

echo "LOCAL_RUNTIME_SCHEMA_READY"
