output "application_hostname" {
  description = "Public hostname protected by Cloudflare Access."
  value       = local.application_hostname
}

output "access_application_id" {
  description = "Cloudflare Access application ID."
  value       = cloudflare_zero_trust_access_application.application.id
}

output "access_policy_id" {
  description = "Cloudflare Access reusable policy ID."
  value       = cloudflare_zero_trust_access_policy.application_users.id
}

output "tunnel_id" {
  description = "Cloudflare Tunnel ID."
  value       = cloudflare_zero_trust_tunnel_cloudflared.application.id
}
