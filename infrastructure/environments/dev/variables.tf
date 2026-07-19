variable "cloudflare_allowed_emails" {
  description = "Exact email addresses allowed by Cloudflare Access. Supply through TF_VAR_cloudflare_allowed_emails."
  type        = set(string)
  sensitive   = true
}

variable "document_bucket_name" {
  description = "Globally unique S3 bucket name used for application documents."
  type        = string
}

variable "cloudflare_account_id" {
  description = "Cloudflare account ID. Supply through TF_VAR_cloudflare_account_id."
  type        = string
}

variable "cloudflare_zone_id" {
  description = "Cloudflare zone ID. Supply through TF_VAR_cloudflare_zone_id."
  type        = string
}

variable "cloudflare_zone_name" {
  description = "Cloudflare-managed DNS zone name."
  type        = string
}

variable "cloudflare_tunnel_name" {
  description = "Cloudflare Tunnel name managed by Terraform."
  type        = string
  default     = "project-admin-dev-ec2"
}

variable "github_repository" {
  description = "GitHub repository trusted by the DEV deployment OIDC role."
  type        = string
  default     = "aki0609-free/project-admin-system"
}

variable "github_repository_owner_id" {
  description = "Immutable numeric GitHub repository owner ID used by the customized OIDC subject."
  type        = string
  default     = "73347656"
}

variable "github_repository_id" {
  description = "Immutable numeric GitHub repository ID used by the customized OIDC subject."
  type        = string
  default     = "1305657877"
}
