variable "role_name" {
  description = "IAM role name assumed by GitHub Actions through OIDC."
  type        = string
}

variable "github_repository" {
  description = "GitHub repository in owner/name format."
  type        = string

  validation {
    condition     = length(split("/", var.github_repository)) == 2
    error_message = "github_repository must use the owner/name format."
  }
}

variable "github_repository_owner_id" {
  description = "Immutable numeric GitHub repository owner ID included in the customized OIDC subject."
  type        = string
}

variable "github_repository_id" {
  description = "Immutable numeric GitHub repository ID included in the customized OIDC subject."
  type        = string
}

variable "github_environment" {
  description = "Protected GitHub Environment allowed to assume the role."
  type        = string
}

variable "aws_region" {
  description = "AWS region containing deployment resources."
  type        = string
}

variable "ecr_repository_arns" {
  description = "ECR repositories to which GitHub Actions may push images."
  type        = set(string)
}

variable "deployment_bucket_arn" {
  description = "S3 bucket ARN used for temporary runtime deployment bundles."
  type        = string
}

variable "application_instance_id" {
  description = "EC2 instance ID targeted by SSM deployment commands."
  type        = string
}

variable "tags" {
  description = "Additional resource tags."
  type        = map(string)
  default     = {}
}
