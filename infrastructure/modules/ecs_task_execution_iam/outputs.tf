output "role_arn" {
  description = "ECSタスク実行ロールARN"
  value       = aws_iam_role.this.arn
}

output "role_name" {
  description = "ECSタスク実行ロール名"
  value       = aws_iam_role.this.name
}
