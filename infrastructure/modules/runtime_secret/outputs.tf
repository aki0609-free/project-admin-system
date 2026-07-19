output "secret_name" {
  description = "Application runtime secret name"
  value       = aws_secretsmanager_secret.this.name
}
