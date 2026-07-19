variable "name_prefix" {
  description = "リソース名の接頭辞"
  type        = string
}

variable "vpc_cidr" {
  description = "VPCのCIDR"
  type        = string
}

variable "app_subnet_cidr" {
  description = "EC2を配置するパブリックサブネットのCIDR"
  type        = string
}

variable "app_availability_zone" {
  description = "EC2を配置するアベイラビリティゾーン"
  type        = string
}

variable "db_subnets" {
  description = "RDS用プライベートサブネット。異なるAZを2つ以上指定する"
  type = list(object({
    cidr_block        = string
    availability_zone = string
  }))

  validation {
    condition     = length(var.db_subnets) >= 2 && length(distinct([for subnet in var.db_subnets : subnet.availability_zone])) >= 2
    error_message = "db_subnetsには異なるAZのサブネットを2つ以上指定してください。"
  }
}

variable "tags" {
  description = "追加タグ"
  type        = map(string)
  default     = {}
}
