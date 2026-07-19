output "role_arn" {
  description = "バックエンドECSタスクロールARN"
  value       = aws_iam_role.this.arn
}

output "role_name" {
  description = "バックエンドECSタスクロール名"
  value       = aws_iam_role.this.name
}

output "policy_arn" {
  description = "書類管理S3アクセス用IAMポリシーARN"
  value       = aws_iam_policy.document_bucket_access.arn
}

output "policy_name" {
  description = "書類管理S3アクセス用IAMポリシー名"
  value       = aws_iam_policy.document_bucket_access.name
}
