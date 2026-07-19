variable "bucket_name" {
  description = "書類管理用S3バケット名"
  type        = string

  validation {
    condition = (
      length(var.bucket_name) >= 3 &&
      length(var.bucket_name) <= 63 &&
      can(regex("^[a-z0-9][a-z0-9.-]*[a-z0-9]$", var.bucket_name))
    )
    error_message = "bucket_nameには3〜63文字の小文字、数字、ピリオド、ハイフンを使用してください。"
  }
}

variable "tags" {
  description = "S3リソースへ追加するタグ"
  type        = map(string)
  default     = {}
}
