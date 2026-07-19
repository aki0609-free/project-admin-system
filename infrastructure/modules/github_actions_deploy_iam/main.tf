data "aws_caller_identity" "current" {}
data "aws_partition" "current" {}

locals {
  github_oidc_host         = "token.actions.githubusercontent.com"
  github_environment_sub   = "repo:${var.github_repository}:environment:${var.github_environment}"
  application_instance_arn = "arn:${data.aws_partition.current.partition}:ec2:${var.aws_region}:${data.aws_caller_identity.current.account_id}:instance/${var.application_instance_id}"
  run_shell_document_arn   = "arn:${data.aws_partition.current.partition}:ssm:${var.aws_region}::document/AWS-RunShellScript"
}

resource "aws_iam_openid_connect_provider" "github" {
  url            = "https://${local.github_oidc_host}"
  client_id_list = ["sts.amazonaws.com"]

  tags = var.tags
}

data "aws_iam_policy_document" "assume_role" {
  statement {
    sid     = "AllowProtectedGitHubEnvironment"
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github.arn]
    }

    condition {
      test     = "StringEquals"
      variable = "${local.github_oidc_host}:aud"
      values   = ["sts.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "${local.github_oidc_host}:sub"
      values   = [local.github_environment_sub]
    }
  }
}

resource "aws_iam_role" "deploy" {
  name                 = var.role_name
  path                 = "/project-admin/"
  description          = "ProjectAdmin DEV deployment role for GitHub Actions OIDC"
  assume_role_policy   = data.aws_iam_policy_document.assume_role.json
  max_session_duration = 3600

  tags = var.tags
}

data "aws_iam_policy_document" "deploy" {
  statement {
    sid       = "AuthorizeEcr"
    effect    = "Allow"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  statement {
    sid    = "PushApplicationImages"
    effect = "Allow"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:BatchGetImage",
      "ecr:CompleteLayerUpload",
      "ecr:DescribeImages",
      "ecr:DescribeImageScanFindings",
      "ecr:GetDownloadUrlForLayer",
      "ecr:InitiateLayerUpload",
      "ecr:PutImage",
      "ecr:UploadLayerPart",
    ]
    resources = var.ecr_repository_arns
  }

  statement {
    sid    = "ReadDeploymentBucket"
    effect = "Allow"
    actions = [
      "s3:GetBucketLocation",
      "s3:ListBucket",
    ]
    resources = [var.deployment_bucket_arn]

    condition {
      test     = "StringLike"
      variable = "s3:prefix"
      values   = ["_deployment/runtime/*"]
    }
  }

  statement {
    sid    = "ManageTemporaryRuntimeBundles"
    effect = "Allow"
    actions = [
      "s3:DeleteObject",
      "s3:GetObject",
      "s3:PutObject",
    ]
    resources = ["${var.deployment_bucket_arn}/_deployment/runtime/*"]
  }

  statement {
    sid    = "InspectDeploymentTarget"
    effect = "Allow"
    actions = [
      "ec2:DescribeInstances",
      "ssm:DescribeInstanceInformation",
      "ssm:GetCommandInvocation",
    ]
    resources = ["*"]
  }

  statement {
    sid     = "RunDeploymentCommand"
    effect  = "Allow"
    actions = ["ssm:SendCommand"]
    resources = [
      local.application_instance_arn,
      local.run_shell_document_arn,
    ]
  }
}

resource "aws_iam_role_policy" "deploy" {
  name   = "${var.role_name}-permissions"
  role   = aws_iam_role.deploy.id
  policy = data.aws_iam_policy_document.deploy.json
}
