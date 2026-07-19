output "identity_arn" {
  description = "ARN of the Amazon SES domain identity."
  value       = aws_ses_domain_identity.this.arn
}

output "domain" {
  description = "Verified Amazon SES sending domain."
  value       = aws_ses_domain_identity.this.domain
}

output "dkim_record_names" {
  description = "Cloudflare DNS names created for Amazon SES Easy DKIM."
  value       = cloudflare_dns_record.dkim[*].name
}
