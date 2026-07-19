variable "domain" {
  description = "Domain verified as an Amazon SES sending identity."
  type        = string
}

variable "cloudflare_zone_id" {
  description = "Cloudflare zone ID where SES DKIM records are created."
  type        = string
}
