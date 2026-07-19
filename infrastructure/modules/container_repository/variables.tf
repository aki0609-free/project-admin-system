variable "repository_name" {
  description = "ECRプライベートリポジトリ名"
  type        = string

  validation {
    condition     = can(regex("^[a-z0-9]+(?:[._/-][a-z0-9]+)*$", var.repository_name))
    error_message = "repository_nameにはECRで使用可能な小文字のリポジトリ名を指定してください。"
  }
}

variable "max_image_count" {
  description = "ECRに保持するイメージの最大数"
  type        = number
  default     = 10

  validation {
    condition     = var.max_image_count >= 1
    error_message = "max_image_countには1以上を指定してください。"
  }
}

variable "untagged_image_expiration_days" {
  description = "タグなしイメージを削除するまでの日数"
  type        = number
  default     = 7

  validation {
    condition     = var.untagged_image_expiration_days >= 1
    error_message = "untagged_image_expiration_daysには1以上を指定してください。"
  }
}

variable "tags" {
  description = "ECRリソースへ追加するタグ"
  type        = map(string)
  default     = {}
}
