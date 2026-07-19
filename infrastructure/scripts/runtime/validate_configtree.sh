#!/usr/bin/env bash
set -euo pipefail

config_dir="${CONFIGTREE_DIR:-/opt/project-admin/config/backend-secrets}"

required_files=(
  "spring.datasource.url"
  "spring.datasource.username"
  "spring.datasource.password"
  "jwt.secret"
  "spring.data.mongodb.uri"
  "cloudflare.tunnel.token"
)

missing=()
for file in "${required_files[@]}"; do
  if [[ ! -s "${config_dir}/${file}" ]]; then
    missing+=("${file}")
  fi
done

if (( ${#missing[@]} > 0 )); then
  echo "Runtime configuration is incomplete. Missing files:" >&2
  printf '  - %s\n' "${missing[@]}" >&2
  exit 1
fi

echo "RUNTIME_CONFIG_OK"
