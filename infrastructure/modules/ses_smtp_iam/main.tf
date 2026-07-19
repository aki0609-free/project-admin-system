resource "aws_iam_user" "this" {
  name = var.user_name
  path = "/project-admin/"
  tags = var.tags
}

data "aws_iam_policy_document" "send_raw_email" {
  statement {
    sid     = "SendRawEmailFromProjectAdmin"
    effect  = "Allow"
    actions = ["ses:SendRawEmail"]
    # SES SMTP evaluates SendRawEmail against identities involved in the
    # request. Keep Resource broad as documented for SMTP, and constrain the
    # usable sender with ses:FromAddress below.
    resources = ["*"]

    condition {
      test     = "StringEquals"
      variable = "ses:FromAddress"
      values   = [var.from_address]
    }
  }
}

resource "aws_iam_user_policy" "send_raw_email" {
  name   = "${var.user_name}-send-raw-email"
  user   = aws_iam_user.this.name
  policy = data.aws_iam_policy_document.send_raw_email.json
}
