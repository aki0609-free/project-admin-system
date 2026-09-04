resource "aws_s3_bucket" "this" {
  bucket = var.bucket_name

  lifecycle {
    prevent_destroy = true
  }

  tags = var.tags
}

resource "aws_s3_bucket_public_access_block" "this" {
  bucket = aws_s3_bucket.this.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_ownership_controls" "this" {
  bucket = aws_s3_bucket.this.id

  rule {
    object_ownership = "BucketOwnerEnforced"
  }
}

resource "aws_s3_bucket_versioning" "this" {
  bucket = aws_s3_bucket.this.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "this" {
  bucket = aws_s3_bucket.this.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "this" {
  bucket = aws_s3_bucket.this.id

  depends_on = [aws_s3_bucket_versioning.this]

  rule {
    id     = "abort-incomplete-multipart-uploads"
    status = "Enabled"

    filter {}

    abort_incomplete_multipart_upload {
      days_after_initiation = 7
    }
  }

  rule {
    id     = "expire-annual-report-backups-after-seven-years"
    status = "Enabled"

    filter {
      prefix = "documents/backups/reports/"
    }

    # 7暦年を下回らないよう、うるう年2日分を含めて保持する。
    expiration {
      days = 2557
    }

    # バージョニング有効バケットでは期限到来後に旧版となるため、
    # さらに30日後に非現行バージョンも削除して保管費用を止める。
    noncurrent_version_expiration {
      noncurrent_days = 30
    }
  }

  rule {
    id     = "expire-company-document-noncurrent-versions"
    status = "Enabled"

    filter {
      prefix = "documents/general/"
    }

    # 現行ファイルは削除せず、差し替え・削除前の旧版だけを1年間保持する。
    noncurrent_version_expiration {
      noncurrent_days = 365
    }

    expiration {
      expired_object_delete_marker = true
    }
  }

  rule {
    id     = "expire-generated-report-noncurrent-versions"
    status = "Enabled"

    filter {
      prefix = "documents/generated-reports/"
    }

    # 確定帳票は年次バックアップへ保存する。ここでは同一キーの旧版だけを整理する。
    noncurrent_version_expiration {
      noncurrent_days = 90
    }

    expiration {
      expired_object_delete_marker = true
    }
  }

  rule {
    id     = "expire-template-noncurrent-versions"
    status = "Enabled"

    filter {
      prefix = "documents/templates/"
    }

    # 誤更新から復元できるようテンプレート旧版は1年間保持する。
    noncurrent_version_expiration {
      noncurrent_days = 365
    }

    expiration {
      expired_object_delete_marker = true
    }
  }

  rule {
    id     = "expire-import-script-noncurrent-versions"
    status = "Enabled"

    filter {
      prefix = "imports/scripts/"
    }

    # 実行資産の追跡・復元用に取込スクリプト旧版を1年間保持する。
    noncurrent_version_expiration {
      noncurrent_days = 365
    }

    expiration {
      expired_object_delete_marker = true
    }
  }

  rule {
    id     = "expire-system-backup-noncurrent-versions"
    status = "Enabled"

    filter {
      prefix = "documents/backups/system/"
    }

    # 現行バックアップは維持し、同じキーへ再生成された場合の旧版だけを整理する。
    noncurrent_version_expiration {
      noncurrent_days = 90
    }

    expiration {
      expired_object_delete_marker = true
    }
  }
}

data "aws_iam_policy_document" "this" {
  statement {
    sid    = "DenyInsecureTransport"
    effect = "Deny"

    principals {
      type        = "*"
      identifiers = ["*"]
    }

    actions = ["s3:*"]

    resources = [
      aws_s3_bucket.this.arn,
      "${aws_s3_bucket.this.arn}/*"
    ]

    condition {
      test     = "Bool"
      variable = "aws:SecureTransport"
      values   = ["false"]
    }
  }
}

resource "aws_s3_bucket_policy" "this" {
  bucket = aws_s3_bucket.this.id
  policy = data.aws_iam_policy_document.this.json

  depends_on = [
    aws_s3_bucket_public_access_block.this
  ]
}
