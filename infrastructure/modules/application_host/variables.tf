variable "name_prefix" {
  description = "リソース名の接頭辞"
  type        = string
}

variable "subnet_id" {
  description = "EC2を配置するsubnet ID"
  type        = string
}

variable "security_group_id" {
  description = "EC2へ割り当てるSecurity Group ID"
  type        = string
}

variable "instance_type" {
  description = "EC2 instance type"
  type        = string
  default     = "t3a.medium"
}

variable "root_volume_size" {
  description = "root EBS volume size (GiB)"
  type        = number
  default     = 30

  validation {
    condition     = var.root_volume_size >= 20
    error_message = "root_volume_sizeは20 GiB以上にしてください。"
  }
}

variable "document_s3_policy_arn" {
  description = "書類管理S3用IAM policy ARN"
  type        = string
}

variable "ecr_repository_arns" {
  description = "EC2からpullを許可するECR repository ARN一覧"
  type        = list(string)

  validation {
    condition     = length(var.ecr_repository_arns) >= 1
    error_message = "ecr_repository_arnsには1件以上のECR repository ARNを指定してください。"
  }
}

variable "tags" {
  description = "追加タグ"
  type        = map(string)
  default     = {}
}
