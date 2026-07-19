output "bucket_arn" {
  description = "書類管理用S3バケットのARN"
  value       = aws_s3_bucket.this.arn
}

output "bucket_id" {
  description = "書類管理用S3バケット名"
  value       = aws_s3_bucket.this.id
}

output "bucket_region" {
  description = "書類管理用S3バケットのリージョン"
  value       = aws_s3_bucket.this.region
}
