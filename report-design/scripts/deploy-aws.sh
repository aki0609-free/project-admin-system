#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_root="$(cd "${script_dir}/../.." && pwd)"
workspace_script="${script_dir}/report-workspace.mjs"
terraform_dir="${project_root}/infrastructure/environments/dev"
schema_script="${project_root}/infrastructure/scripts/database/apply_runtime_schema_upgrade.sh"
manifest="$(mktemp "${TMPDIR:-/tmp}/project-admin-report-schema.XXXXXX")"
trap 'rm -f "${manifest}"' EXIT

cd "${project_root}"
node "${workspace_script}" sync

echo "JasperテンプレートをBackendと同じJasperReportsで事前検証します。"
(
  cd "${project_root}/backend"
  ./gradlew test --tests '*JasperTemplateTest'
)

export AWS_PROFILE="${AWS_PROFILE:-project-admin-terraform}"
export AWS_REGION="${AWS_REGION:-ap-northeast-1}"
export AWS_PAGER=""

for command in aws terraform; do
  command -v "${command}" >/dev/null 2>&1 || {
    echo "必要なコマンドがありません: ${command}" >&2
    exit 1
  }
done

aws sts get-caller-identity >/dev/null
bucket_name="$(terraform -chdir="${terraform_dir}" output -raw document_bucket_name)"

if [[ "${REPORT_DEPLOY_CONFIRM:-}" != "DEPLOY AWS" ]]; then
  echo "反映先: AWS開発環境 (${bucket_name})"
  read -r -p "帳票テンプレートと帳票用SQLを反映します。DEPLOY AWS と入力してください: " confirmation
  [[ "${confirmation}" == "DEPLOY AWS" ]] || {
    echo "AWS反映を中止しました。"
    exit 1
  }
fi

node "${workspace_script}" list --kind=sql --format=resource > "${manifest}"
echo "帳票用View・ストアドをAWS DBへ反映します。"
RUNTIME_SCHEMA_MANIFEST="${manifest}" bash "${schema_script}"

echo "Jasperテンプレートをバージョン管理付きS3へ反映します。"
while IFS= read -r working_path || [[ -n "${working_path}" ]]; do
  [[ -z "${working_path}" ]] && continue
  file_name="$(basename "${working_path}")"
  aws s3api put-object \
    --bucket "${bucket_name}" \
    --key "documents/templates/reports/${file_name}" \
    --body "${project_root}/${working_path}" \
    --content-type application/xml \
    --query '{VersionId:VersionId,ETag:ETag}' \
    --output json
done < <(node "${workspace_script}" list --kind=jasper --format=working)

echo "AWS_REPORT_DEPLOY_COMPLETE"
echo "S3のバージョニングにより、反映前のJasperも復元できます。"
