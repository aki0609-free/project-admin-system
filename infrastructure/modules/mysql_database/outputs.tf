output "instance_identifier" {
  description = "RDS DB instance identifier"
  value       = aws_db_instance.this.identifier
}

output "address" {
  description = "RDS endpoint address"
  value       = aws_db_instance.this.address
}

output "port" {
  description = "RDS endpoint port"
  value       = aws_db_instance.this.port
}

output "database_name" {
  description = "Application database name"
  value       = aws_db_instance.this.db_name
}

output "application_secret_name" {
  description = "Application DB credential secret name"
  value       = aws_secretsmanager_secret.application.name
}
