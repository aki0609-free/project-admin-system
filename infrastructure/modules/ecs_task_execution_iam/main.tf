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
  description          = "ProjectAdmin ECS task execution role"
  assume_role_policy   = data.aws_iam_policy_document.ecs_task_assume_role.json
  max_session_duration = 3600

  tags = var.tags
}

resource "aws_iam_role_policy_attachment" "ecs_task_execution" {
  role = aws_iam_role.this.name
  policy_arn = format(
    "arn:%s:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy",
    data.aws_partition.current.partition
  )
}
