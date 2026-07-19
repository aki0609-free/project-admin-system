output "state_bucket_name" {
  description = "Terraform stateを保存するS3バケット名"
  value       = aws_s3_bucket.terraform_state.id
}

output "state_bucket_region" {
  description = "Terraform state用S3バケットのリージョン"
  value       = aws_s3_bucket.terraform_state.region
}
