module "document_bucket" {
  source = "../../modules/document_bucket"

  bucket_name = local.document_bucket_name

  tags = {
    Component = "DocumentManagement"
    DataType  = "BusinessDocuments"
  }
}

module "backend_runtime_iam" {
  source = "../../modules/backend_runtime_iam"

  role_name           = local.backend_task_role_name
  policy_name         = local.backend_s3_policy_name
  aws_region          = "ap-northeast-1"
  document_bucket_arn = module.document_bucket.bucket_arn

  tags = {
    Component = "Backend"
    Purpose   = "DocumentStorageAccess"
  }
}

module "backend_container_repository" {
  source = "../../modules/container_repository"

  repository_name                = local.backend_ecr_name
  max_image_count                = 10
  untagged_image_expiration_days = 7

  tags = {
    Component = "Backend"
    Purpose   = "ContainerImages"
  }
}

module "frontend_container_repository" {
  source = "../../modules/container_repository"

  repository_name                = local.frontend_ecr_name
  max_image_count                = 10
  untagged_image_expiration_days = 7

  tags = {
    Component = "Frontend"
    Purpose   = "ContainerImages"
  }
}

module "ecs_task_execution_iam" {
  source = "../../modules/ecs_task_execution_iam"

  role_name  = local.task_execution_role_name
  aws_region = "ap-northeast-1"

  tags = {
    Component = "ECS"
    Purpose   = "TaskExecution"
  }
}

module "network_foundation" {
  source = "../../modules/network_foundation"

  name_prefix           = local.name_prefix
  vpc_cidr              = "10.20.0.0/16"
  app_subnet_cidr       = "10.20.0.0/24"
  app_availability_zone = "ap-northeast-1a"

  db_subnets = [
    {
      cidr_block        = "10.20.10.0/24"
      availability_zone = "ap-northeast-1a"
    },
    {
      cidr_block        = "10.20.11.0/24"
      availability_zone = "ap-northeast-1c"
    }
  ]

  tags = {
    Purpose = "EC2RDSFoundation"
  }
}

module "application_host" {
  source = "../../modules/application_host"

  name_prefix            = local.name_prefix
  subnet_id              = module.network_foundation.app_subnet_id
  security_group_id      = module.network_foundation.app_security_group_id
  instance_type          = "t3a.medium"
  root_volume_size       = 30
  document_s3_policy_arn = module.backend_runtime_iam.policy_arn
  ecr_repository_arns = [
    module.backend_container_repository.repository_arn,
    module.frontend_container_repository.repository_arn,
  ]

  tags = {
    Component = "ApplicationHost"
    Purpose   = "EC2DockerRuntime"
  }
}

module "mysql_database" {
  source = "../../modules/mysql_database"

  name_prefix             = local.name_prefix
  db_subnet_ids           = module.network_foundation.db_subnet_ids
  db_security_group_id    = module.network_foundation.db_security_group_id
  application_role_name   = module.application_host.instance_role_name
  engine_version          = "8.0.46"
  instance_class          = "db.t4g.micro"
  database_name           = "ADMIN"
  master_username         = "projectadmin"
  allocated_storage       = 20
  max_allocated_storage   = 50
  backup_retention_period = 7

  tags = {
    Component = "Database"
    Purpose   = "ApplicationMySQL"
  }
}

module "application_runtime_secret" {
  source = "../../modules/runtime_secret"

  secret_name           = "project-admin/dev/application/runtime"
  application_role_name = module.application_host.instance_role_name
  policy_name           = "${local.name_prefix}-runtime-secret-read"

  tags = {
    Component = "ApplicationRuntime"
    Purpose   = "RuntimeSecrets"
  }
}

module "cloudflare_zero_trust" {
  source = "../../modules/cloudflare_zero_trust"

  account_id              = local.cloudflare_account_id
  zone_id                 = local.cloudflare_zone_id
  zone_name               = local.cloudflare_zone_name
  application_subdomain   = "project-admin"
  tunnel_name             = local.cloudflare_tunnel_name
  origin_service          = "http://frontend:8080"
  access_application_name = "project-admin"
  access_policy_name      = "Allow ProjectAdmin DEV users"
  allowed_emails          = var.cloudflare_allowed_emails
}

module "github_actions_deploy_iam" {
  source = "../../modules/github_actions_deploy_iam"

  role_name          = "${local.name_prefix}-github-actions-deploy"
  github_repository  = var.github_repository
  github_environment = "dev"
  aws_region         = "ap-northeast-1"
  ecr_repository_arns = [
    module.backend_container_repository.repository_arn,
    module.frontend_container_repository.repository_arn,
  ]
  deployment_bucket_arn   = module.document_bucket.bucket_arn
  application_instance_id = module.application_host.instance_id

  tags = {
    Component = "DeploymentAutomation"
    Purpose   = "GitHubActionsOIDC"
  }
}
