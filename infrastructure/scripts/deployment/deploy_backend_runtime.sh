#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_root="$(cd "${script_dir}/../../.." && pwd)"
terraform_dir="${project_root}/infrastructure/environments/dev"
compose_file="${project_root}/infrastructure/runtime/dev/compose.yaml"
prepare_script="${project_root}/infrastructure/scripts/runtime/prepare_configtree.py"
validate_script="${project_root}/infrastructure/scripts/runtime/validate_configtree.sh"
install_script="${script_dir}/install_runtime_bundle.sh"

export AWS_PROFILE="${AWS_PROFILE:-project-admin-terraform}"
export AWS_REGION="${AWS_REGION:-ap-northeast-1}"
export AWS_PAGER=""

backend_image="${BACKEND_IMAGE:-${1:-}}"
frontend_image="${FRONTEND_IMAGE:-${2:-}}"
if [[ -z "${backend_image}" || -z "${frontend_image}" ]]; then
  echo "Usage: BACKEND_IMAGE=<immutable backend ECR image> FRONTEND_IMAGE=<immutable frontend ECR image> $0" >&2
  exit 1
fi

for command in aws terraform tar; do
  if ! command -v "${command}" >/dev/null 2>&1; then
    echo "Required command is missing: ${command}" >&2
    exit 1
  fi
done

repository_url="$(terraform -chdir="${terraform_dir}" output -raw backend_ecr_repository_url)"
repository_name="$(terraform -chdir="${terraform_dir}" output -raw backend_ecr_repository_name)"
frontend_repository_url="$(terraform -chdir="${terraform_dir}" output -raw frontend_ecr_repository_url)"
frontend_repository_name="$(terraform -chdir="${terraform_dir}" output -raw frontend_ecr_repository_name)"
instance_id="$(terraform -chdir="${terraform_dir}" output -raw app_instance_id)"
bucket_name="$(terraform -chdir="${terraform_dir}" output -raw document_bucket_name)"

case "${backend_image}" in
  "${repository_url}":*) ;;
  *)
    echo "BACKEND_IMAGE must use the managed ECR repository: ${repository_url}" >&2
    exit 1
    ;;
esac

case "${frontend_image}" in
  "${frontend_repository_url}":*) ;;
  *)
    echo "FRONTEND_IMAGE must use the managed ECR repository: ${frontend_repository_url}" >&2
    exit 1
    ;;
esac

image_tag="${backend_image#${repository_url}:}"
aws ecr describe-images \
  --repository-name "${repository_name}" \
  --image-ids "imageTag=${image_tag}" >/dev/null

frontend_image_tag="${frontend_image#${frontend_repository_url}:}"
aws ecr describe-images \
  --repository-name "${frontend_repository_name}" \
  --image-ids "imageTag=${frontend_image_tag}" >/dev/null

instance_state="$(aws ec2 describe-instances \
  --instance-ids "${instance_id}" \
  --query 'Reservations[0].Instances[0].State.Name' \
  --output text)"
if [[ "${instance_state}" != "running" ]]; then
  echo "EC2 must be running. Current state: ${instance_state}" >&2
  exit 1
fi

ssm_status="$(aws ssm describe-instance-information \
  --filters "Key=InstanceIds,Values=${instance_id}" \
  --query 'InstanceInformationList[0].PingStatus' \
  --output text)"
if [[ "${ssm_status}" != "Online" ]]; then
  echo "EC2 SSM agent must be Online. Current status: ${ssm_status}" >&2
  exit 1
fi

work_dir="$(mktemp -d "${TMPDIR:-/tmp}/project-admin-deploy.XXXXXX")"
bundle_key="_deployment/runtime/backend-$(date -u +%Y%m%d-%H%M%SZ).tar.gz"
bundle_uri="s3://${bucket_name}/${bundle_key}"
uploaded=false

cleanup() {
  if [[ "${uploaded}" == "true" ]]; then
    aws s3 rm "${bundle_uri}" >/dev/null 2>&1 || true
  fi
  rm -rf "${work_dir}"
}
trap cleanup EXIT

install -d \
  "${work_dir}/bundle/runtime" \
  "${work_dir}/bundle/scripts/deployment" \
  "${work_dir}/bundle/scripts/runtime"

install -m 0640 "${compose_file}" "${work_dir}/bundle/runtime/compose.yaml"
install -m 0750 "${install_script}" "${work_dir}/bundle/scripts/deployment/install_runtime_bundle.sh"
install -m 0750 "${prepare_script}" "${work_dir}/bundle/scripts/runtime/prepare_configtree.py"
install -m 0750 "${validate_script}" "${work_dir}/bundle/scripts/runtime/validate_configtree.sh"

printf '%s\n' \
  "BACKEND_IMAGE=${backend_image}" \
  "FRONTEND_IMAGE=${frontend_image}" \
  "SPRING_PROFILES_ACTIVE=aws" \
  "APP_AI_ENABLED=false" \
  "PROJECT_STORAGE_S3_BUCKET=${bucket_name}" \
  > "${work_dir}/bundle/runtime/deployment.env"
chmod 0600 "${work_dir}/bundle/runtime/deployment.env"

COPYFILE_DISABLE=1 tar --no-xattrs \
  -C "${work_dir}/bundle" \
  -czf "${work_dir}/runtime-bundle.tar.gz" .

echo "Uploading temporary runtime bundle..."
aws s3 cp "${work_dir}/runtime-bundle.tar.gz" "${bundle_uri}" >/dev/null
uploaded=true

remote_archive="/tmp/project-admin-runtime-bundle.tar.gz"
remote_dir="/tmp/project-admin-runtime-bundle"
remote_command="set -euo pipefail; rm -rf ${remote_dir}; install -d -m 0700 ${remote_dir}; aws s3 cp ${bundle_uri} ${remote_archive}; tar -xzf ${remote_archive} -C ${remote_dir}; bash ${remote_dir}/scripts/deployment/install_runtime_bundle.sh"

echo "Deploying runtime bundle through SSM..."
command_id="$(aws ssm send-command \
  --instance-ids "${instance_id}" \
  --document-name AWS-RunShellScript \
  --comment "ProjectAdmin backend runtime deployment" \
  --parameters "commands=[\"${remote_command}\"]" \
  --query 'Command.CommandId' \
  --output text)"

set +e
aws ssm wait command-executed \
  --command-id "${command_id}" \
  --instance-id "${instance_id}"
wait_result=$?
set -e

aws ssm get-command-invocation \
  --command-id "${command_id}" \
  --instance-id "${instance_id}" \
  --query '{Status:Status,Output:StandardOutputContent,Error:StandardErrorContent}' \
  --output json

if [[ ${wait_result} -ne 0 ]]; then
  echo "Runtime deployment failed. Command ID: ${command_id}" >&2
  exit 1
fi

echo "BACKEND_RUNTIME_DEPLOYED"
echo "BACKEND_IMAGE=${backend_image}"
echo "FRONTEND_IMAGE=${frontend_image}"
