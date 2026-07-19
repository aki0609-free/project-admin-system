output "document_bucket_arn" {
  description = "書類管理用S3バケットのARN"
  value       = module.document_bucket.bucket_arn
}

output "document_bucket_name" {
  description = "書類管理用S3バケット名"
  value       = module.document_bucket.bucket_id
}

output "document_bucket_region" {
  description = "書類管理用S3バケットのリージョン"
  value       = module.document_bucket.bucket_region
}

output "backend_task_role_arn" {
  description = "バックエンドECSタスクロールARN"
  value       = module.backend_runtime_iam.role_arn
}

output "backend_task_role_name" {
  description = "バックエンドECSタスクロール名"
  value       = module.backend_runtime_iam.role_name
}

output "backend_s3_policy_arn" {
  description = "バックエンド用書類管理S3アクセスIAMポリシーARN"
  value       = module.backend_runtime_iam.policy_arn
}

output "backend_ecr_repository_arn" {
  description = "バックエンド用ECRプライベートリポジトリARN"
  value       = module.backend_container_repository.repository_arn
}

output "backend_ecr_repository_name" {
  description = "バックエンド用ECRプライベートリポジトリ名"
  value       = module.backend_container_repository.repository_name
}

output "backend_ecr_repository_url" {
  description = "バックエンドコンテナイメージのpush先ECR URL"
  value       = module.backend_container_repository.repository_url
}

output "frontend_ecr_repository_arn" {
  description = "Frontend ECR private repository ARN"
  value       = module.frontend_container_repository.repository_arn
}

output "frontend_ecr_repository_name" {
  description = "Frontend ECR private repository name"
  value       = module.frontend_container_repository.repository_name
}

output "frontend_ecr_repository_url" {
  description = "Frontend container image push destination ECR URL"
  value       = module.frontend_container_repository.repository_url
}

output "ecs_task_execution_role_arn" {
  description = "ECSタスク実行ロールARN"
  value       = module.ecs_task_execution_iam.role_arn
}

output "ecs_task_execution_role_name" {
  description = "ECSタスク実行ロール名"
  value       = module.ecs_task_execution_iam.role_name
}

output "vpc_id" {
  description = "ProjectAdmin dev VPC ID"
  value       = module.network_foundation.vpc_id
}

output "app_subnet_id" {
  description = "EC2用パブリックサブネットID"
  value       = module.network_foundation.app_subnet_id
}

output "db_subnet_ids" {
  description = "RDS用プライベートサブネットID一覧"
  value       = module.network_foundation.db_subnet_ids
}

output "app_security_group_id" {
  description = "EC2用セキュリティグループID"
  value       = module.network_foundation.app_security_group_id
}

output "db_security_group_id" {
  description = "RDS用セキュリティグループID"
  value       = module.network_foundation.db_security_group_id
}

output "app_instance_id" {
  description = "Application EC2 instance ID"
  value       = module.application_host.instance_id
}

output "app_instance_role_name" {
  description = "Application EC2 IAM role name"
  value       = module.application_host.instance_role_name
}

output "app_private_ip" {
  description = "Application EC2 private IP"
  value       = module.application_host.private_ip
}

output "app_elastic_ip" {
  description = "MongoDB Atlas IP Access Listへ登録するElastic IP"
  value       = module.application_host.elastic_ip
}

output "mysql_instance_identifier" {
  description = "RDS MySQL instance identifier"
  value       = module.mysql_database.instance_identifier
}

output "mysql_address" {
  description = "RDS MySQL endpoint address"
  value       = module.mysql_database.address
}

output "mysql_port" {
  description = "RDS MySQL endpoint port"
  value       = module.mysql_database.port
}

output "mysql_database_name" {
  description = "RDS MySQL database name"
  value       = module.mysql_database.database_name
}

output "mysql_application_secret_name" {
  description = "Application DB credential secret name"
  value       = module.mysql_database.application_secret_name
}

output "application_runtime_secret_name" {
  description = "Application runtime secret name"
  value       = module.application_runtime_secret.secret_name
}
output "cloudflare_application_hostname" {
  description = "ProjectAdmin public hostname protected by Cloudflare Access."
  value       = module.cloudflare_zero_trust.application_hostname
}

output "cloudflare_tunnel_id" {
  description = "Cloudflare Tunnel ID."
  value       = module.cloudflare_zero_trust.tunnel_id
}

output "github_actions_deploy_role_arn" {
  description = "IAM role ARN assumed by the protected GitHub dev Environment."
  value       = module.github_actions_deploy_iam.role_arn
}

output "github_actions_deploy_role_name" {
  description = "GitHub Actions deployment IAM role name."
  value       = module.github_actions_deploy_iam.role_name
}
