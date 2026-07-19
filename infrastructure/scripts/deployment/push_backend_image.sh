#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_root="$(cd "${script_dir}/../../.." && pwd)"
backend_dir="${project_root}/backend"
terraform_dir="${project_root}/infrastructure/environments/dev"

export AWS_PROFILE="${AWS_PROFILE:-project-admin-terraform}"
export AWS_REGION="${AWS_REGION:-ap-northeast-1}"

for command in aws docker terraform; do
  if ! command -v "${command}" >/dev/null 2>&1; then
    echo "Required command is missing: ${command}" >&2
    exit 1
  fi
done

if ! docker info >/dev/null 2>&1; then
  echo "Docker Desktop is not running." >&2
  exit 1
fi

repository_url="$(terraform -chdir="${terraform_dir}" output -raw backend_ecr_repository_url)"
repository_name="$(terraform -chdir="${terraform_dir}" output -raw backend_ecr_repository_name)"
registry="${repository_url%%/*}"
image_tag="${IMAGE_TAG:-manual-$(date -u +%Y%m%d-%H%M%SZ)}"

if [[ ! "${image_tag}" =~ ^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$ ]]; then
  echo "Invalid ECR image tag: ${image_tag}" >&2
  exit 1
fi

if aws ecr describe-images \
  --repository-name "${repository_name}" \
  --image-ids "imageTag=${image_tag}" >/dev/null 2>&1; then
  echo "ECR image tag already exists and the repository is immutable: ${image_tag}" >&2
  exit 1
fi

echo "Logging in to ECR..."
aws ecr get-login-password \
  | docker login --username AWS --password-stdin "${registry}" >/dev/null

image_reference="${repository_url}:${image_tag}"

echo "Building and pushing linux/amd64 backend image..."
docker buildx build \
  --platform linux/amd64 \
  --provenance=false \
  --tag "${image_reference}" \
  --push \
  "${backend_dir}"

image_digest="$(aws ecr describe-images \
  --repository-name "${repository_name}" \
  --image-ids "imageTag=${image_tag}" \
  --query 'imageDetails[0].imageDigest' \
  --output text)"

echo "BACKEND_IMAGE_PUSHED"
echo "BACKEND_IMAGE=${image_reference}"
echo "IMAGE_DIGEST=${image_digest}"
