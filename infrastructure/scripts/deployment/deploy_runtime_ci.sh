#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_root="$(cd "${script_dir}/../../.." && pwd)"
compose_file="${project_root}/infrastructure/runtime/dev/compose.yaml"
prepare_script="${project_root}/infrastructure/scripts/runtime/prepare_configtree.py"
validate_script="${project_root}/infrastructure/scripts/runtime/validate_configtree.sh"
install_script="${script_dir}/install_runtime_bundle.sh"

export AWS_PAGER=""

required_variables=(AWS_REGION APP_INSTANCE_ID DOCUMENT_BUCKET_NAME BACKEND_IMAGE FRONTEND_IMAGE)
for variable_name in "${required_variables[@]}"; do
  if [[ -z "${!variable_name:-}" ]]; then
    echo "Required environment variable is missing: ${variable_name}" >&2
    exit 1
  fi
done

for command in aws curl jq tar; do
  if ! command -v "${command}" >/dev/null 2>&1; then
    echo "Required command is missing: ${command}" >&2
    exit 1
  fi
done

backend_repository_name="${BACKEND_IMAGE%:*}"
backend_repository_name="${backend_repository_name##*/}"
backend_image_tag="${BACKEND_IMAGE##*:}"
frontend_repository_name="${FRONTEND_IMAGE%:*}"
frontend_repository_name="${frontend_repository_name##*/}"
frontend_image_tag="${FRONTEND_IMAGE##*:}"

aws ecr describe-images --repository-name "${backend_repository_name}" --image-ids "imageTag=${backend_image_tag}" >/dev/null
aws ecr describe-images --repository-name "${frontend_repository_name}" --image-ids "imageTag=${frontend_image_tag}" >/dev/null

instance_state="$(aws ec2 describe-instances --instance-ids "${APP_INSTANCE_ID}" --query 'Reservations[0].Instances[0].State.Name' --output text)"
if [[ "${instance_state}" != "running" ]]; then
  echo "EC2 must be running. Current state: ${instance_state}" >&2
  exit 1
fi

ssm_status="$(aws ssm describe-instance-information --filters "Key=InstanceIds,Values=${APP_INSTANCE_ID}" --query 'InstanceInformationList[0].PingStatus' --output text)"
if [[ "${ssm_status}" != "Online" ]]; then
  echo "EC2 SSM agent must be Online. Current status: ${ssm_status}" >&2
  exit 1
fi

work_dir="$(mktemp -d "${TMPDIR:-/tmp}/project-admin-ci-deploy.XXXXXX")"
bundle_key="_deployment/runtime/github-${GITHUB_RUN_ID:-manual}-${GITHUB_RUN_ATTEMPT:-1}-$(date -u +%Y%m%d-%H%M%SZ).tar.gz"
bundle_uri="s3://${DOCUMENT_BUCKET_NAME}/${bundle_key}"
uploaded=false

cleanup() {
  if [[ "${uploaded}" == "true" ]]; then
    aws s3 rm "${bundle_uri}" >/dev/null 2>&1 || true
  fi
  rm -rf "${work_dir}"
}
trap cleanup EXIT

install -d "${work_dir}/bundle/runtime" "${work_dir}/bundle/scripts/deployment" "${work_dir}/bundle/scripts/runtime"
install -m 0640 "${compose_file}" "${work_dir}/bundle/runtime/compose.yaml"
install -m 0750 "${install_script}" "${work_dir}/bundle/scripts/deployment/install_runtime_bundle.sh"
install -m 0750 "${prepare_script}" "${work_dir}/bundle/scripts/runtime/prepare_configtree.py"
install -m 0750 "${validate_script}" "${work_dir}/bundle/scripts/runtime/validate_configtree.sh"

printf '%s\n' \
  "BACKEND_IMAGE=${BACKEND_IMAGE}" \
  "FRONTEND_IMAGE=${FRONTEND_IMAGE}" \
  "SPRING_PROFILES_ACTIVE=aws" \
  "APP_AI_ENABLED=false" \
  "PROJECT_STORAGE_S3_BUCKET=${DOCUMENT_BUCKET_NAME}" \
  > "${work_dir}/bundle/runtime/deployment.env"
chmod 0600 "${work_dir}/bundle/runtime/deployment.env"
tar -C "${work_dir}/bundle" -czf "${work_dir}/runtime-bundle.tar.gz" .

echo "Uploading temporary runtime bundle..."
aws s3 cp "${work_dir}/runtime-bundle.tar.gz" "${bundle_uri}" >/dev/null
uploaded=true

remote_archive="/tmp/project-admin-runtime-bundle.tar.gz"
remote_dir="/tmp/project-admin-runtime-bundle"
remote_command="set -euo pipefail; rm -rf ${remote_dir}; install -d -m 0700 ${remote_dir}; aws s3 cp ${bundle_uri} ${remote_archive}; tar -xzf ${remote_archive} -C ${remote_dir}; bash ${remote_dir}/scripts/deployment/install_runtime_bundle.sh; cd /opt/project-admin/runtime; docker compose --env-file deployment.env up -d --remove-orphans --wait --wait-timeout 360; curl --fail --silent --show-error http://127.0.0.1:8080/actuator/health; docker compose --env-file deployment.env ps"
parameters_file="${work_dir}/ssm-parameters.json"
jq -n --arg command "${remote_command}" '{commands: [$command]}' > "${parameters_file}"

echo "Deploying application through AWS Systems Manager..."
command_id="$(aws ssm send-command \
  --instance-ids "${APP_INSTANCE_ID}" \
  --document-name AWS-RunShellScript \
  --comment "ProjectAdmin GitHub Actions DEV deployment" \
  --timeout-seconds 900 \
  --parameters "file://${parameters_file}" \
  --query 'Command.CommandId' \
  --output text)"

status="Pending"
for _ in $(seq 1 150); do
  status="$(aws ssm get-command-invocation --command-id "${command_id}" --instance-id "${APP_INSTANCE_ID}" --query Status --output text 2>/dev/null || printf 'Pending')"
  case "${status}" in
    Success|Failed|Cancelled|TimedOut|Cancelling) break ;;
  esac
  sleep 5
done

aws ssm get-command-invocation \
  --command-id "${command_id}" \
  --instance-id "${APP_INSTANCE_ID}" \
  --query '{Status:Status,Output:StandardOutputContent,Error:StandardErrorContent}' \
  --output json

if [[ "${status}" != "Success" ]]; then
  echo "Runtime deployment failed with status ${status}. Command ID: ${command_id}" >&2
  exit 1
fi

echo "DEV_DEPLOYMENT_SUCCEEDED"
echo "BACKEND_IMAGE=${BACKEND_IMAGE}"
echo "FRONTEND_IMAGE=${FRONTEND_IMAGE}"
