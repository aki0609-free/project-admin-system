variable "role_name" {
  description = "ECSタスク実行ロール名"
  type        = string
}

variable "aws_region" {
  description = "ECSタスクを実行するAWSリージョン"
  type        = string
}

variable "tags" {
  description = "IAMロールへ追加するタグ"
  type        = map(string)
  default     = {}
}
