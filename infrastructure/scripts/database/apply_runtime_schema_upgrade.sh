#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_root="$(cd "${script_dir}/../../.." && pwd)"
terraform_dir="${project_root}/infrastructure/environments/dev"
remote_runner="${script_dir}/run_runtime_schema_upgrade_sql.sh"
schema_manifest="${project_root}/backend/src/main/resources/sql/runtime-schema-manifest.txt"
sql_files=()

while IFS= read -r resource_path || [[ -n "${resource_path}" ]]; do
  [[ -z "${resource_path}" || "${resource_path}" == \#* ]] && continue
  sql_files+=("${project_root}/backend/src/main/resources/${resource_path}")
done < "${schema_manifest}"

export AWS_PROFILE="${AWS_PROFILE:-project-admin-terraform}"
export AWS_REGION="${AWS_REGION:-ap-northeast-1}"
export AWS_PAGER=""

for command in aws terraform tar; do
  command -v "${command}" >/dev/null 2>&1 || {
    echo "Required command is missing: ${command}" >&2
    exit 1
  }
done

for file in "${remote_runner}" "${schema_manifest}" "${sql_files[@]}"; do
  [[ -f "${file}" ]] || {
    echo "Required file is missing: ${file}" >&2
    exit 1
  }
done

instance_id="$(terraform -chdir="${terraform_dir}" output -raw app_instance_id)"
instance_role_name="$(terraform -chdir="${terraform_dir}" output -raw app_instance_role_name)"
bucket_name="$(terraform -chdir="${terraform_dir}" output -raw document_bucket_name)"
db_identifier="$(terraform -chdir="${terraform_dir}" output -raw mysql_instance_identifier)"
database_name="$(terraform -chdir="${terraform_dir}" output -raw mysql_database_name)"
database_host="$(terraform -chdir="${terraform_dir}" output -raw mysql_address)"
database_port="$(terraform -chdir="${terraform_dir}" output -raw mysql_port)"

db_status="$(aws rds describe-db-instances \
  --db-instance-identifier "${db_identifier}" \
  --query 'DBInstances[0].DBInstanceStatus' \
  --output text)"
[[ "${db_status}" == "available" ]] || {
  echo "RDS must be available. Current status: ${db_status}" >&2
  exit 1
}

master_secret_arn="$(aws rds describe-db-instances \
  --db-instance-identifier "${db_identifier}" \
  --query 'DBInstances[0].MasterUserSecret.SecretArn' \
  --output text)"
[[ -n "${master_secret_arn}" && "${master_secret_arn}" != "None" ]] || {
  echo "RDS managed master secret ARN was not found." >&2
  exit 1
}

ssm_status="$(aws ssm describe-instance-information \
  --filters "Key=InstanceIds,Values=${instance_id}" \
  --query 'InstanceInformationList[0].PingStatus' \
  --output text)"
[[ "${ssm_status}" == "Online" ]] || {
  echo "EC2 SSM agent must be Online. Current status: ${ssm_status}" >&2
  exit 1
}

work_dir="$(mktemp -d "${TMPDIR:-/tmp}/project-admin-schema.XXXXXX")"
bundle_name="runtime-schema-upgrade-$(date -u +%Y%m%d-%H%M%SZ)"
bundle_key="_deployment/database/${bundle_name}.tar.gz"
bundle_uri="s3://${bucket_name}/${bundle_key}"
policy_name="project-admin-dev-temporary-runtime-schema-secret-read"
uploaded=false
policy_created=false

cleanup() {
  [[ "${uploaded}" != "true" ]] || aws s3 rm "${bundle_uri}" >/dev/null 2>&1 || true
  if [[ "${policy_created}" == "true" ]]; then
    aws iam delete-role-policy \
      --role-name "${instance_role_name}" \
      --policy-name "${policy_name}" >/dev/null 2>&1 || true
  fi
  rm -rf "${work_dir}"
}
trap cleanup EXIT

install -d "${work_dir}/bundle"
install -m 0750 "${remote_runner}" "${work_dir}/bundle/run.sh"
remote_sql_files=()
index=1
for sql_file in "${sql_files[@]}"; do
  remote_name="$(printf '%02d-%s' "${index}" "$(basename "${sql_file}")")"
  install -m 0640 "${sql_file}" "${work_dir}/bundle/${remote_name}"
  remote_sql_files+=("/tmp/${bundle_name}/${remote_name}")
  index=$((index + 1))
done

COPYFILE_DISABLE=1 tar --no-xattrs \
  -C "${work_dir}/bundle" \
  -czf "${work_dir}/${bundle_name}.tar.gz" .

policy_document="$(printf '%s' \
  '{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Action":["secretsmanager:DescribeSecret","secretsmanager:GetSecretValue"],"Resource":"' \
  "${master_secret_arn}" \
  '"}]}')"
aws iam put-role-policy \
  --role-name "${instance_role_name}" \
  --policy-name "${policy_name}" \
  --policy-document "${policy_document}"
policy_created=true

aws s3 cp "${work_dir}/${bundle_name}.tar.gz" "${bundle_uri}" >/dev/null
uploaded=true

remote_dir="/tmp/${bundle_name}"
remote_archive="/tmp/${bundle_name}.tar.gz"
printf -v sql_arguments ' %q' "${remote_sql_files[@]}"
remote_command="set -euo pipefail; rm -rf ${remote_dir}; install -d -m 0700 ${remote_dir}; aws s3 cp ${bundle_uri} ${remote_archive}; tar -xzf ${remote_archive} -C ${remote_dir}; chmod 0750 ${remote_dir}/run.sh; ${remote_dir}/run.sh ${master_secret_arn} ${database_host} ${database_port} ${database_name}${sql_arguments}; rm -rf ${remote_dir} ${remote_archive}"

command_id="$(aws ssm send-command \
  --instance-ids "${instance_id}" \
  --document-name AWS-RunShellScript \
  --comment "ProjectAdmin runtime schema upgrade" \
  --parameters "commands=[\"${remote_command}\"]" \
  --query 'Command.CommandId' \
  --output text)"

command_status="Pending"
deadline=$((SECONDS + 600))
while true; do
  command_status="$(aws ssm get-command-invocation \
    --command-id "${command_id}" \
    --instance-id "${instance_id}" \
    --query Status --output text 2>/dev/null || true)"
  case "${command_status}" in
    Success|Failed|Cancelled|TimedOut|Cancelling) break ;;
    Pending|InProgress|Delayed|"")
      ((SECONDS < deadline)) || { command_status="ClientTimeout"; break; }
      sleep 5
      ;;
    *) echo "Unexpected SSM status: ${command_status}" >&2; exit 1 ;;
  esac
done

aws ssm get-command-invocation \
  --command-id "${command_id}" \
  --instance-id "${instance_id}" \
  --query '{Status:Status,Output:StandardOutputContent,Error:StandardErrorContent}' \
  --output json

[[ "${command_status}" == "Success" ]] || {
  echo "Runtime schema upgrade failed. Command ID: ${command_id}" >&2
  exit 1
}

echo "RUNTIME_SCHEMA_UPGRADE_COMPLETE"
