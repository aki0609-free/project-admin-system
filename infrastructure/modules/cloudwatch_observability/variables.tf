variable "name_prefix" {
  description = "Resource name prefix."
  type        = string
}

variable "log_group_name" {
  description = "CloudWatch Logs group used by the Docker runtime."
  type        = string
}

variable "log_retention_days" {
  description = "CloudWatch Logs retention period in days."
  type        = number
  default     = 14

  validation {
    condition     = contains([1, 3, 5, 7, 14, 30, 60, 90, 120, 150, 180, 365], var.log_retention_days)
    error_message = "log_retention_days must be a CloudWatch Logs supported retention value."
  }
}

variable "application_role_name" {
  description = "EC2 instance role allowed to publish container logs."
  type        = string
}

variable "application_instance_id" {
  description = "Application EC2 instance ID displayed in the dashboard."
  type        = string
}

variable "database_instance_identifier" {
  description = "RDS instance identifier displayed in the dashboard."
  type        = string
}

variable "aws_region" {
  description = "AWS region containing the monitored resources."
  type        = string
}

variable "tags" {
  description = "Additional resource tags."
  type        = map(string)
  default     = {}
}
