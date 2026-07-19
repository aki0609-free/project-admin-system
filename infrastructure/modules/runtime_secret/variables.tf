variable "secret_name" {
  description = "Application runtime secret name"
  type        = string
}

variable "application_role_name" {
  description = "Secret read permissionを付与するEC2 IAM role name"
  type        = string
}

variable "policy_name" {
  description = "EC2 IAM inline policy name"
  type        = string
}

variable "tags" {
  description = "追加タグ"
  type        = map(string)
  default     = {}
}
