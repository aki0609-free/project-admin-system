locals {
  document_bucket_name     = var.document_bucket_name
  backend_task_role_name   = "project-admin-dev-backend-task"
  backend_s3_policy_name   = "project-admin-dev-backend-document-s3"
  backend_ecr_name         = "project-admin-dev-backend"
  frontend_ecr_name        = "project-admin-dev-frontend"
  task_execution_role_name = "project-admin-dev-ecs-task-execution"
  name_prefix              = "project-admin-dev"
  cloudflare_account_id    = var.cloudflare_account_id
  cloudflare_zone_id       = var.cloudflare_zone_id
  cloudflare_zone_name     = var.cloudflare_zone_name
  cloudflare_tunnel_name   = var.cloudflare_tunnel_name
}
