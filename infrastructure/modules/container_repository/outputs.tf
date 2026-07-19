output "repository_arn" {
  description = "ECRプライベートリポジトリARN"
  value       = aws_ecr_repository.this.arn
}

output "repository_name" {
  description = "ECRプライベートリポジトリ名"
  value       = aws_ecr_repository.this.name
}

output "repository_url" {
  description = "コンテナイメージのpush先ECR URL"
  value       = aws_ecr_repository.this.repository_url
}
