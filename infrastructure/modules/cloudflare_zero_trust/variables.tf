variable "account_id" {
  description = "Cloudflare account ID."
  type        = string
}

variable "zone_id" {
  description = "Cloudflare zone ID for the application domain."
  type        = string
}

variable "zone_name" {
  description = "Cloudflare zone name."
  type        = string
}

variable "application_subdomain" {
  description = "Subdomain exposed through Cloudflare Tunnel."
  type        = string
}

variable "tunnel_name" {
  description = "Cloudflare Tunnel name."
  type        = string
}

variable "origin_service" {
  description = "Service URL resolved from the cloudflared container network."
  type        = string
}

variable "access_application_name" {
  description = "Cloudflare Access application name."
  type        = string
}

variable "access_policy_name" {
  description = "Reusable Cloudflare Access policy name."
  type        = string
}

variable "allowed_emails" {
  description = "Exact email addresses allowed by Cloudflare Access. Never commit real values."
  type        = set(string)
  sensitive   = true

  validation {
    condition     = length(var.allowed_emails) > 0 && alltrue([for email in var.allowed_emails : can(regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", email))])
    error_message = "allowed_emails must contain at least one valid email address."
  }
}
