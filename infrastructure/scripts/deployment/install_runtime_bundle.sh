#!/usr/bin/env bash
set -euo pipefail

bundle_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

install -d -o root -g root -m 0750 \
  /opt/project-admin/runtime \
  /opt/project-admin/scripts \
  /opt/project-admin/config

install -d -o 10001 -g 10001 -m 0750 \
  /opt/project-admin/logs

install -o root -g root -m 0640 \
  "${bundle_dir}/runtime/compose.yaml" \
  /opt/project-admin/runtime/compose.yaml

install -o root -g root -m 0600 \
  "${bundle_dir}/runtime/deployment.env" \
  /opt/project-admin/runtime/deployment.env

install -o root -g root -m 0750 \
  "${bundle_dir}/scripts/runtime/prepare_configtree.py" \
  /opt/project-admin/scripts/prepare_configtree.py

install -o root -g root -m 0750 \
  "${bundle_dir}/scripts/runtime/validate_configtree.sh" \
  /opt/project-admin/scripts/validate_configtree.sh

AWS_REGION="${AWS_REGION:-ap-northeast-1}" \
  python3 /opt/project-admin/scripts/prepare_configtree.py

/opt/project-admin/scripts/validate_configtree.sh

backend_image="$(sed -n 's/^BACKEND_IMAGE=//p' /opt/project-admin/runtime/deployment.env)"
if [[ -z "${backend_image}" ]]; then
  echo "BACKEND_IMAGE is missing from deployment.env" >&2
  exit 1
fi

frontend_image="$(sed -n 's/^FRONTEND_IMAGE=//p' /opt/project-admin/runtime/deployment.env)"
if [[ -z "${frontend_image}" ]]; then
  echo "FRONTEND_IMAGE is missing from deployment.env" >&2
  exit 1
fi

registry="${backend_image%%/*}"
docker_config_dir="$(mktemp -d /tmp/project-admin-docker-config.XXXXXX)"

cleanup_docker_config() {
  rm -rf "${docker_config_dir}"
}
trap cleanup_docker_config EXIT

export DOCKER_CONFIG="${docker_config_dir}"
aws ecr get-login-password --region "${AWS_REGION:-ap-northeast-1}" \
  | docker login --username AWS --password-stdin "${registry}" >/dev/null

cd /opt/project-admin/runtime
docker compose --env-file deployment.env config --quiet
docker compose --env-file deployment.env pull
docker logout "${registry}" >/dev/null

cleanup_docker_config
trap - EXIT

echo "RUNTIME_DEPLOYMENT_READY"
