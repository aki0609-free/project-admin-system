output "user_name" {
  description = "Dedicated Amazon SES SMTP IAM user name."
  value       = aws_iam_user.this.name
}

output "user_arn" {
  description = "Dedicated Amazon SES SMTP IAM user ARN."
  value       = aws_iam_user.this.arn
}
