#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
terraform_dir="$(cd "${script_dir}/../../environments/dev" && pwd)"

export AWS_PROFILE="${AWS_PROFILE:-project-admin-terraform}"
export AWS_REGION="${AWS_REGION:-ap-northeast-1}"

instance_id="$(terraform -chdir="${terraform_dir}" output -raw app_instance_id)"
db_identifier="$(terraform -chdir="${terraform_dir}" output -raw mysql_instance_identifier)"

wait_for_rds_state() {
  local target_state="$1"
  local current_state

  for _ in {1..80}; do
    current_state="$(aws rds describe-db-instances \
      --db-instance-identifier "${db_identifier}" \
      --query 'DBInstances[0].DBInstanceStatus' \
      --output text)"

    if [[ "${current_state}" == "${target_state}" ]]; then
      return 0
    fi

    sleep 15
  done

  echo "Timed out waiting for RDS state: ${target_state}" >&2
  return 1
}

instance_state="$(aws ec2 describe-instances --instance-ids "${instance_id}" --query 'Reservations[0].Instances[0].State.Name' --output text)"
if [[ "${instance_state}" == "running" ]]; then
  echo "Stopping EC2..."
  aws ec2 stop-instances --instance-ids "${instance_id}" >/dev/null
  aws ec2 wait instance-stopped --instance-ids "${instance_id}"
elif [[ "${instance_state}" != "stopped" ]]; then
  echo "EC2 is not ready to stop: ${instance_state}" >&2
  exit 1
fi

db_state="$(aws rds describe-db-instances --db-instance-identifier "${db_identifier}" --query 'DBInstances[0].DBInstanceStatus' --output text)"
if [[ "${db_state}" == "available" ]]; then
  echo "Stopping RDS..."
  aws rds stop-db-instance --db-instance-identifier "${db_identifier}" >/dev/null
  echo "Waiting for RDS to stop..."
  wait_for_rds_state "stopped"
elif [[ "${db_state}" == "stopping" ]]; then
  echo "RDS is already stopping. Waiting for completion..."
  wait_for_rds_state "stopped"
elif [[ "${db_state}" != "stopped" ]]; then
  echo "RDS is not ready to stop: ${db_state}" >&2
  exit 1
fi

echo "DEV_RESOURCES_STOPPED"
