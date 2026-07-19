variable "name_prefix" {
  description = "リソース名の接頭辞"
  type        = string
}

variable "db_subnet_ids" {
  description = "RDSを配置するPrivate subnet ID一覧"
  type        = list(string)

  validation {
    condition     = length(var.db_subnet_ids) >= 2
    error_message = "RDS用subnetを2つ以上指定してください。"
  }
}

variable "db_security_group_id" {
  description = "RDSへ割り当てるSecurity Group ID"
  type        = string
}

variable "application_role_name" {
  description = "Application EC2 IAM role name"
  type        = string
}

variable "engine_version" {
  description = "RDS MySQL engine version"
  type        = string
  default     = "8.0.46"
}

variable "instance_class" {
  description = "RDS DB instance class"
  type        = string
  default     = "db.t4g.micro"
}

variable "database_name" {
  description = "初期database name"
  type        = string
  default     = "ADMIN"
}

variable "master_username" {
  description = "RDS master username。passwordはRDSに管理させる"
  type        = string
  default     = "projectadmin"
}

variable "allocated_storage" {
  description = "初期storage size (GiB)"
  type        = number
  default     = 20
}

variable "max_allocated_storage" {
  description = "storage autoscaling upper limit (GiB)"
  type        = number
  default     = 50
}

variable "backup_retention_period" {
  description = "automated backup retention days"
  type        = number
  default     = 7
}

variable "tags" {
  description = "追加タグ"
  type        = map(string)
  default     = {}
}
