data "aws_caller_identity" "current" {}

data "aws_partition" "current" {}

data "aws_iam_policy_document" "ecs_task_assume_role" {
  statement {
    sid     = "AllowEcsTasksToAssumeRole"
    effect  = "Allow"
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:SourceAccount"
      values   = [data.aws_caller_identity.current.account_id]
    }

    condition {
      test     = "ArnLike"
      variable = "aws:SourceArn"
      values = [
        "arn:${data.aws_partition.current.partition}:ecs:${var.aws_region}:${data.aws_caller_identity.current.account_id}:*"
      ]
    }
  }
}

resource "aws_iam_role" "this" {
  name                 = var.role_name
  path                 = "/project-admin/"
  description          = "ProjectAdmin backend ECS task role"
  assume_role_policy   = data.aws_iam_policy_document.ecs_task_assume_role.json
  max_session_duration = 3600

  tags = var.tags
}

data "aws_iam_policy_document" "document_bucket_access" {
  statement {
    sid    = "ReadDocumentBucketMetadata"
    effect = "Allow"

    actions = [
      "s3:GetBucketLocation",
      "s3:ListBucket",
      "s3:ListBucketMultipartUploads"
    ]

    resources = [var.document_bucket_arn]

    condition {
      test     = "StringEquals"
      variable = "aws:ResourceAccount"
      values   = [data.aws_caller_identity.current.account_id]
    }
  }

  statement {
    sid    = "ManageDocumentObjects"
    effect = "Allow"

    actions = [
      "s3:GetObject",
      "s3:PutObject",
      "s3:DeleteObject",
      "s3:AbortMultipartUpload",
      "s3:ListMultipartUploadParts"
    ]

    resources = ["${var.document_bucket_arn}/*"]

    condition {
      test     = "StringEquals"
      variable = "aws:ResourceAccount"
      values   = [data.aws_caller_identity.current.account_id]
    }
  }
}

resource "aws_iam_policy" "document_bucket_access" {
  name        = var.policy_name
  path        = "/project-admin/"
  description = "Least-privilege access to the ProjectAdmin document bucket"
  policy      = data.aws_iam_policy_document.document_bucket_access.json

  tags = var.tags
}

resource "aws_iam_role_policy_attachment" "document_bucket_access" {
  role       = aws_iam_role.this.name
  policy_arn = aws_iam_policy.document_bucket_access.arn
}
