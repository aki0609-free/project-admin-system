variable "user_name" {
  description = "Dedicated IAM user name used only for Amazon SES SMTP."
  type        = string
}

variable "from_address" {
  description = "Exact From address permitted for the SMTP user."
  type        = string
}

variable "tags" {
  description = "Tags applied to the SMTP IAM user."
  type        = map(string)
  default     = {}
}
