output "log_group_name" {
  description = "CloudWatch Logs group receiving runtime container logs."
  value       = aws_cloudwatch_log_group.runtime.name
}

output "dashboard_name" {
  description = "CloudWatch operations dashboard name."
  value       = aws_cloudwatch_dashboard.runtime.dashboard_name
}

output "alarm_names" {
  description = "CloudWatch alarms created for the DEV runtime."
  value = [
    aws_cloudwatch_metric_alarm.backend_errors.alarm_name,
    aws_cloudwatch_metric_alarm.ec2_status_check.alarm_name,
    aws_cloudwatch_metric_alarm.rds_free_storage.alarm_name,
  ]
}
