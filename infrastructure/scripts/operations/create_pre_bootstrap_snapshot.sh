#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
terraform_dir="$(cd "${script_dir}/../../environments/dev" && pwd)"

export AWS_PROFILE="${AWS_PROFILE:-project-admin-terraform}"
export AWS_REGION="${AWS_REGION:-ap-northeast-1}"
export AWS_PAGER=""

db_identifier="$(terraform -chdir="${terraform_dir}" output -raw mysql_instance_identifier)"
snapshot_identifier="${SNAPSHOT_IDENTIFIER:-project-admin-dev-before-hibernate-bootstrap-$(date -u +%Y%m%d-%H%M%SZ)}"

db_state="$(aws rds describe-db-instances \
  --db-instance-identifier "${db_identifier}" \
  --query 'DBInstances[0].DBInstanceStatus' \
  --output text)"

if [[ "${db_state}" != "available" ]]; then
  echo "RDS must be available before snapshot. Current state: ${db_state}" >&2
  exit 1
fi

if aws rds describe-db-snapshots \
  --db-snapshot-identifier "${snapshot_identifier}" >/dev/null 2>&1; then
  echo "DB snapshot already exists: ${snapshot_identifier}" >&2
  exit 1
fi

echo "Creating pre-bootstrap RDS snapshot..."
aws rds create-db-snapshot \
  --db-instance-identifier "${db_identifier}" \
  --db-snapshot-identifier "${snapshot_identifier}" \
  --tags \
    Key=Project,Value=ProjectAdminSystem \
    Key=Environment,Value=dev \
    Key=Purpose,Value=PreHibernateBootstrap \
    Key=ManagedBy,Value=Manual >/dev/null

aws rds wait db-snapshot-completed \
  --db-snapshot-identifier "${snapshot_identifier}"

snapshot_arn="$(aws rds describe-db-snapshots \
  --db-snapshot-identifier "${snapshot_identifier}" \
  --query 'DBSnapshots[0].DBSnapshotArn' \
  --output text)"

echo "PRE_BOOTSTRAP_SNAPSHOT_READY"
echo "SNAPSHOT_IDENTIFIER=${snapshot_identifier}"
echo "SNAPSHOT_ARN=${snapshot_arn}"
