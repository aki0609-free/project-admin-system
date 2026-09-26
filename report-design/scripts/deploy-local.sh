#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_root="$(cd "${script_dir}/../.." && pwd)"
workspace_script="${script_dir}/report-workspace.mjs"
sql_manifest="$(mktemp "${TMPDIR:-/tmp}/project-admin-report-schema.XXXXXX")"
jasper_manifest="$(mktemp "${TMPDIR:-/tmp}/project-admin-report-jasper.XXXXXX")"
trap 'rm -f "${sql_manifest}" "${jasper_manifest}"' EXIT

cd "${project_root}"
node "${workspace_script}" sync

echo "JasperテンプレートをBackendと同じJasperReportsで事前検証します。"
(
  cd "${project_root}/backend"
  ./gradlew test --tests '*JasperTemplateTest'
)

for command in docker curl; do
  command -v "${command}" >/dev/null 2>&1 || {
    echo "必要なコマンドがありません: ${command}" >&2
    exit 1
  }
done

docker info >/dev/null 2>&1 || {
  echo "Docker Desktopを起動してください。" >&2
  exit 1
}

node "${workspace_script}" list --kind=sql --format=resource > "${sql_manifest}"
node "${workspace_script}" list --kind=jasper --format=resource > "${jasper_manifest}"

wait_for_backend() {
  local attempt
  for attempt in $(seq 1 90); do
    if curl --fail --silent http://127.0.0.1:8080/actuator/health >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done
  docker compose logs --no-color --tail=120 backend || true
  echo "Backendの起動確認に失敗しました。" >&2
  return 1
}

echo "LocalのBackendを帳票テンプレート込みで更新します。"
docker compose up -d mysql mongodb redis mailpit
docker compose up -d --build backend
wait_for_backend

# /app/storage is a persistent volume. Rebuilding the image alone does not
# replace templates already present there, so explicitly synchronize the
# registered Jasper assets after the container has started.
echo "帳票テンプレートをLocalの永続領域へ反映します。"
while IFS= read -r resource_path || [[ -n "${resource_path}" ]]; do
  [[ -z "${resource_path}" || "${resource_path}" == \#* ]] && continue
  jasper_file="${project_root}/backend/src/main/resources/${resource_path}"
  docker compose cp "${jasper_file}" "backend:/app/storage/${resource_path}" >/dev/null
done < "${jasper_manifest}"

echo "帳票用View・ストアドをLocal DBへ反映します。"
while IFS= read -r resource_path || [[ -n "${resource_path}" ]]; do
  [[ -z "${resource_path}" || "${resource_path}" == \#* ]] && continue
  sql_file="${project_root}/backend/src/main/resources/${resource_path}"
  docker compose exec -T mysql sh -c \
    'exec mysql --user=root --password="$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' \
    < "${sql_file}"
done < "${sql_manifest}"

docker compose restart backend >/dev/null
wait_for_backend

echo "LOCAL_REPORT_DEPLOY_COMPLETE"
echo "確認URL: http://localhost:5173"
