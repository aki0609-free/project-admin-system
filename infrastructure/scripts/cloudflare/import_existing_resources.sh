#!/usr/bin/env bash

set -euo pipefail

ACCOUNT_ID="${CLOUDFLARE_ACCOUNT_ID:-}"
ZONE_NAME="${CLOUDFLARE_ZONE_NAME:-}"
TUNNEL_ID="${CLOUDFLARE_TUNNEL_ID:-}"
ACCESS_APPLICATION_ID="${CLOUDFLARE_ACCESS_APPLICATION_ID:-}"
ACCESS_POLICY_ID="${CLOUDFLARE_ACCESS_POLICY_ID:-}"
APPLICATION_HOSTNAME="${CLOUDFLARE_APPLICATION_HOSTNAME:-}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEV_DIR="$(cd "${SCRIPT_DIR}/../../environments/dev" && pwd)"

required_variables=(
  CLOUDFLARE_API_TOKEN
  CLOUDFLARE_ACCOUNT_ID
  CLOUDFLARE_ZONE_NAME
  CLOUDFLARE_TUNNEL_ID
  CLOUDFLARE_ACCESS_APPLICATION_ID
  CLOUDFLARE_ACCESS_POLICY_ID
  CLOUDFLARE_APPLICATION_HOSTNAME
  TF_VAR_cloudflare_allowed_emails
)

for variable_name in "${required_variables[@]}"; do
  if [[ -z "${!variable_name:-}" ]]; then
    echo "Required environment variable is not set: ${variable_name}" >&2
    exit 1
  fi
done

export AWS_PROFILE="${AWS_PROFILE:-project-admin-terraform}"

cloudflare_get() {
  local endpoint="$1"

  curl --fail --silent --show-error \
    --header "Authorization: Bearer ${CLOUDFLARE_API_TOKEN}" \
    --header "Content-Type: application/json" \
    "https://api.cloudflare.com/client/v4${endpoint}"
}

verify_result="$(cloudflare_get "/user/tokens/verify" | jq -r '.success')"
if [[ "${verify_result}" != "true" ]]; then
  echo "Cloudflare API token verification failed." >&2
  exit 1
fi

zone_id="$(
  cloudflare_get "/zones?name=${ZONE_NAME}&account.id=${ACCOUNT_ID}" \
    | jq -r '[.result[] | .id] | if length == 1 then .[0] else empty end'
)"

if [[ -z "${zone_id}" ]]; then
  echo "Expected exactly one Cloudflare zone for ${ZONE_NAME}." >&2
  exit 1
fi

one_time_pin_id="$(
  cloudflare_get "/accounts/${ACCOUNT_ID}/access/identity_providers" \
    | jq -r '[.result[] | select(.type == "onetimepin") | .id] | if length == 1 then .[0] else empty end'
)"

if [[ -z "${one_time_pin_id}" ]]; then
  echo "Expected exactly one One-time PIN identity provider." >&2
  exit 1
fi

dns_record_id="$(
  cloudflare_get "/zones/${zone_id}/dns_records?type=CNAME&name=${APPLICATION_HOSTNAME}" \
    | jq -r '[.result[] | .id] | if length == 1 then .[0] else empty end'
)"

if [[ -z "${dns_record_id}" ]]; then
  echo "Expected exactly one CNAME record for ${APPLICATION_HOSTNAME}." >&2
  exit 1
fi

cd "${DEV_DIR}"

import_if_missing() {
  local address="$1"
  local import_id="$2"

  if terraform state show "${address}" >/dev/null 2>&1; then
    echo "Already imported: ${address}"
    return
  fi

  terraform import "${address}" "${import_id}"
}

import_if_missing \
  'module.cloudflare_zero_trust.cloudflare_zero_trust_tunnel_cloudflared.application' \
  "${ACCOUNT_ID}/${TUNNEL_ID}"

import_if_missing \
  'module.cloudflare_zero_trust.cloudflare_zero_trust_access_identity_provider.one_time_pin' \
  "accounts/${ACCOUNT_ID}/${one_time_pin_id}"

import_if_missing \
  'module.cloudflare_zero_trust.cloudflare_zero_trust_access_policy.application_users' \
  "${ACCOUNT_ID}/${ACCESS_POLICY_ID}"

import_if_missing \
  'module.cloudflare_zero_trust.cloudflare_zero_trust_access_application.application' \
  "accounts/${ACCOUNT_ID}/${ACCESS_APPLICATION_ID}"

import_if_missing \
  'module.cloudflare_zero_trust.cloudflare_zero_trust_tunnel_cloudflared_config.application' \
  "${ACCOUNT_ID}/${TUNNEL_ID}"

import_if_missing \
  'module.cloudflare_zero_trust.cloudflare_dns_record.application' \
  "${zone_id}/${dns_record_id}"

echo "CLOUDFLARE_RESOURCES_IMPORTED"
