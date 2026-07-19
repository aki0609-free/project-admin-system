resource "aws_ses_domain_identity" "this" {
  domain = var.domain
}

resource "aws_ses_domain_dkim" "this" {
  domain = aws_ses_domain_identity.this.domain
}

# SES Easy DKIM always issues three tokens. A fixed count keeps the resource
# addresses known during the first plan even though AWS returns the tokens only
# after creating the identity.
resource "cloudflare_dns_record" "dkim" {
  count = 3

  zone_id = var.cloudflare_zone_id
  name    = "${tolist(aws_ses_domain_dkim.this.dkim_tokens)[count.index]}._domainkey.${var.domain}"
  type    = "CNAME"
  content = "${tolist(aws_ses_domain_dkim.this.dkim_tokens)[count.index]}.dkim.amazonses.com"
  proxied = false
  ttl     = 300
  comment = "Amazon SES DKIM record managed by Terraform"
}
