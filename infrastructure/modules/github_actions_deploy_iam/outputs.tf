output "role_arn" {
  description = "IAM role ARN configured as the GitHub Environment variable AWS_DEPLOY_ROLE_ARN."
  value       = aws_iam_role.deploy.arn
}

output "role_name" {
  description = "GitHub Actions deployment IAM role name."
  value       = aws_iam_role.deploy.name
}

output "oidc_provider_arn" {
  description = "GitHub Actions OIDC provider ARN."
  value       = aws_iam_openid_connect_provider.github.arn
}
