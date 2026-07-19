output "instance_id" {
  description = "Application EC2 instance ID"
  value       = aws_instance.this.id
}

output "instance_role_name" {
  description = "Application EC2 IAM role name"
  value       = aws_iam_role.this.name
}

output "private_ip" {
  description = "Application EC2 private IP"
  value       = aws_instance.this.private_ip
}

output "elastic_ip" {
  description = "Atlas IP Access Listへ登録するElastic IP"
  value       = aws_eip.this.public_ip
}
