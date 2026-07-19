output "vpc_id" {
  description = "VPC ID"
  value       = aws_vpc.this.id
}

output "app_subnet_id" {
  description = "EC2用パブリックサブネットID"
  value       = aws_subnet.app.id
}

output "db_subnet_ids" {
  description = "RDS用プライベートサブネットID一覧"
  value       = [for key in sort(keys(aws_subnet.db)) : aws_subnet.db[key].id]
}

output "app_security_group_id" {
  description = "EC2用セキュリティグループID"
  value       = aws_security_group.app.id
}

output "db_security_group_id" {
  description = "RDS用セキュリティグループID"
  value       = aws_security_group.db.id
}
