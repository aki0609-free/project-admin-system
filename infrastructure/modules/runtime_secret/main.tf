resource "aws_secretsmanager_secret" "this" {
  name                    = var.secret_name
  description             = "ProjectAdmin application runtime credentials. Secret value is registered outside Terraform."
  recovery_window_in_days = 7

  tags = var.tags
}

data "aws_iam_policy_document" "read" {
  statement {
    sid    = "ReadApplicationRuntimeSecret"
    effect = "Allow"
    actions = [
      "secretsmanager:DescribeSecret",
      "secretsmanager:GetSecretValue"
    ]
    resources = [aws_secretsmanager_secret.this.arn]
  }
}

resource "aws_iam_role_policy" "read" {
  name   = var.policy_name
  role   = var.application_role_name
  policy = data.aws_iam_policy_document.read.json
}
