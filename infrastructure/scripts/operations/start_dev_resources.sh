#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
terraform_dir="$(cd "${script_dir}/../../environments/dev" && pwd)"

export AWS_PROFILE="${AWS_PROFILE:-project-admin-terraform}"
export AWS_REGION="${AWS_REGION:-ap-northeast-1}"

instance_id="$(terraform -chdir="${terraform_dir}" output -raw app_instance_id)"
db_identifier="$(terraform -chdir="${terraform_dir}" output -raw mysql_instance_identifier)"

db_state="$(aws rds describe-db-instances --db-instance-identifier "${db_identifier}" --query 'DBInstances[0].DBInstanceStatus' --output text)"
if [[ "${db_state}" == "stopped" ]]; then
  echo "Starting RDS..."
  aws rds start-db-instance --db-instance-identifier "${db_identifier}" >/dev/null
  aws rds wait db-instance-available --db-instance-identifier "${db_identifier}"
elif [[ "${db_state}" != "available" ]]; then
  echo "RDS is not ready to start: ${db_state}" >&2
  exit 1
fi

instance_state="$(aws ec2 describe-instances --instance-ids "${instance_id}" --query 'Reservations[0].Instances[0].State.Name' --output text)"
if [[ "${instance_state}" == "stopped" ]]; then
  echo "Starting EC2..."
  aws ec2 start-instances --instance-ids "${instance_id}" >/dev/null
  aws ec2 wait instance-running --instance-ids "${instance_id}"
elif [[ "${instance_state}" != "running" ]]; then
  echo "EC2 is not ready to start: ${instance_state}" >&2
  exit 1
fi

echo "DEV_RESOURCES_STARTED"
