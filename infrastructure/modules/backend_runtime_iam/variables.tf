variable "role_name" {
  description = "バックエンドECSタスクロール名"
  type        = string
}

variable "policy_name" {
  description = "書類管理S3アクセス用IAMポリシー名"
  type        = string
}

variable "aws_region" {
  description = "ECSタスクを実行するAWSリージョン"
  type        = string
}

variable "document_bucket_arn" {
  description = "アクセスを許可する書類管理S3バケットARN"
  type        = string

  validation {
    condition     = can(regex("^arn:[^:]+:s3:::[^/]+$", var.document_bucket_arn))
    error_message = "document_bucket_arnにはS3バケットARNを指定してください。"
  }
}

variable "tags" {
  description = "IAMリソースへ追加するタグ"
  type        = map(string)
  default     = {}
}
