data "aws_partition" "current" {}

resource "aws_cloudwatch_log_group" "runtime" {
  name              = var.log_group_name
  retention_in_days = var.log_retention_days

  tags = var.tags
}

data "aws_iam_policy_document" "log_writer" {
  statement {
    sid    = "PublishRuntimeLogs"
    effect = "Allow"
    actions = [
      "logs:CreateLogStream",
      "logs:DescribeLogStreams",
      "logs:PutLogEvents",
    ]
    resources = ["${aws_cloudwatch_log_group.runtime.arn}:*"]
  }
}

resource "aws_iam_role_policy" "log_writer" {
  name   = "${var.name_prefix}-cloudwatch-logs-write"
  role   = var.application_role_name
  policy = data.aws_iam_policy_document.log_writer.json
}

resource "aws_cloudwatch_log_metric_filter" "backend_errors" {
  name           = "${var.name_prefix}-backend-errors"
  log_group_name = aws_cloudwatch_log_group.runtime.name
  pattern        = "{ ($.app = \"backend\") && ($.level = \"ERROR\") }"

  metric_transformation {
    name          = "BackendErrorCount"
    namespace     = "ProjectAdmin/Dev"
    value         = "1"
    default_value = "0"
    unit          = "Count"
  }
}

resource "aws_cloudwatch_metric_alarm" "backend_errors" {
  alarm_name          = "${var.name_prefix}-backend-errors"
  alarm_description   = "Backend emitted one or more ERROR level logs in five minutes."
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 1
  threshold           = 1
  metric_name         = "BackendErrorCount"
  namespace           = "ProjectAdmin/Dev"
  period              = 300
  statistic           = "Sum"
  treat_missing_data  = "notBreaching"

  tags = var.tags
}

resource "aws_cloudwatch_metric_alarm" "ec2_status_check" {
  alarm_name          = "${var.name_prefix}-ec2-status-check"
  alarm_description   = "Application EC2 instance failed an AWS status check."
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 2
  threshold           = 1
  metric_name         = "StatusCheckFailed"
  namespace           = "AWS/EC2"
  period              = 300
  statistic           = "Maximum"
  treat_missing_data  = "notBreaching"

  dimensions = {
    InstanceId = var.application_instance_id
  }

  tags = var.tags
}

resource "aws_cloudwatch_metric_alarm" "rds_free_storage" {
  alarm_name          = "${var.name_prefix}-rds-free-storage"
  alarm_description   = "RDS free storage is below 2 GiB."
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = 2
  threshold           = 2147483648
  metric_name         = "FreeStorageSpace"
  namespace           = "AWS/RDS"
  period              = 300
  statistic           = "Average"
  treat_missing_data  = "notBreaching"

  dimensions = {
    DBInstanceIdentifier = var.database_instance_identifier
  }

  tags = var.tags
}

resource "aws_cloudwatch_dashboard" "runtime" {
  dashboard_name = "${var.name_prefix}-operations"

  dashboard_body = jsonencode({
    widgets = [
      {
        type   = "text"
        x      = 0
        y      = 0
        width  = 24
        height = 2
        properties = {
          markdown = "# ProjectAdmin DEV\nRuntime health, infrastructure metrics, and application errors."
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 2
        width  = 8
        height = 6
        properties = {
          title  = "EC2 CPU / Status"
          region = var.aws_region
          stat   = "Average"
          period = 300
          metrics = [
            ["AWS/EC2", "CPUUtilization", "InstanceId", var.application_instance_id],
            [".", "StatusCheckFailed", ".", ".", { stat = "Maximum", yAxis = "right" }],
          ]
          yAxis = {
            left  = { min = 0, max = 100 }
            right = { min = 0 }
          }
        }
      },
      {
        type   = "metric"
        x      = 8
        y      = 2
        width  = 8
        height = 6
        properties = {
          title  = "RDS CPU / Connections"
          region = var.aws_region
          stat   = "Average"
          period = 300
          metrics = [
            ["AWS/RDS", "CPUUtilization", "DBInstanceIdentifier", var.database_instance_identifier],
            [".", "DatabaseConnections", ".", ".", { yAxis = "right" }],
          ]
          yAxis = {
            left  = { min = 0, max = 100 }
            right = { min = 0 }
          }
        }
      },
      {
        type   = "metric"
        x      = 16
        y      = 2
        width  = 8
        height = 6
        properties = {
          title  = "Backend ERROR count"
          region = var.aws_region
          stat   = "Sum"
          period = 300
          metrics = [
            ["ProjectAdmin/Dev", "BackendErrorCount"],
          ]
          yAxis = {
            left = { min = 0 }
          }
        }
      },
      {
        type   = "log"
        x      = 0
        y      = 8
        width  = 24
        height = 8
        properties = {
          title  = "Recent backend errors"
          region = var.aws_region
          view   = "table"
          query  = "SOURCE '${aws_cloudwatch_log_group.runtime.name}' | fields @timestamp, traceId, logger_name, message | filter level = 'ERROR' | sort @timestamp desc | limit 50"
        }
      },
    ]
  })
}
