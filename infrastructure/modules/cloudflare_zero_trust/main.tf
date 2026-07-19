locals {
  application_hostname = "${var.application_subdomain}.${var.zone_name}"
}

resource "cloudflare_zero_trust_tunnel_cloudflared" "application" {
  account_id = var.account_id
  name       = var.tunnel_name
  config_src = "cloudflare"
}

resource "cloudflare_zero_trust_access_identity_provider" "one_time_pin" {
  account_id = var.account_id
  # Cloudflare represents the account-level One-time PIN provider with an empty
  # custom name and renders the provider type as its display label.
  name   = ""
  type   = "onetimepin"
  config = {}
}

resource "cloudflare_zero_trust_access_policy" "application_users" {
  account_id       = var.account_id
  name             = var.access_policy_name
  decision         = "allow"
  session_duration = "24h"

  connection_rules = {
    rdp = {}
  }

  include = [
    for email in sort(tolist(var.allowed_emails)) : {
      email = {
        email = email
      }
    }
  ]
}

resource "cloudflare_zero_trust_access_application" "application" {
  account_id                 = var.account_id
  name                       = var.access_application_name
  domain                     = local.application_hostname
  type                       = "self_hosted"
  session_duration           = "24h"
  auto_redirect_to_identity  = false
  enable_binding_cookie      = false
  http_only_cookie_attribute = false
  options_preflight_bypass   = false

  policies = [{
    id         = cloudflare_zero_trust_access_policy.application_users.id
    precedence = 1
  }]
}

resource "cloudflare_zero_trust_tunnel_cloudflared_config" "application" {
  account_id = var.account_id
  tunnel_id  = cloudflare_zero_trust_tunnel_cloudflared.application.id
  source     = "cloudflare"

  config = {
    ingress = [
      {
        hostname       = local.application_hostname
        service        = var.origin_service
        origin_request = {}
      },
      {
        service = "http_status:404"
      },
    ]
  }
}

resource "cloudflare_dns_record" "application" {
  zone_id = var.zone_id
  name    = local.application_hostname
  type    = "CNAME"
  content = "${cloudflare_zero_trust_tunnel_cloudflared.application.id}.cfargotunnel.com"
  proxied = true
  ttl     = 1
}
