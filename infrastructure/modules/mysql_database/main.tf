resource "aws_db_subnet_group" "this" {
  name        = "${var.name_prefix}-mysql"
  description = "Private subnets for ProjectAdmin MySQL"
  subnet_ids  = var.db_subnet_ids

  tags = merge(var.tags, {
    Name = "${var.name_prefix}-mysql"
  })
}

resource "aws_db_parameter_group" "this" {
  name        = "${var.name_prefix}-mysql80"
  family      = "mysql8.0"
  description = "ProjectAdmin MySQL 8.0 parameters"

  parameter {
    name         = "time_zone"
    value        = "Asia/Tokyo"
    apply_method = "immediate"
  }

  parameter {
    name         = "slow_query_log"
    value        = "1"
    apply_method = "immediate"
  }

  parameter {
    name         = "long_query_time"
    value        = "2"
    apply_method = "immediate"
  }

  tags = var.tags
}

resource "aws_cloudwatch_log_group" "mysql_error" {
  name              = "/aws/rds/instance/${var.name_prefix}-mysql/error"
  retention_in_days = 14

  tags = var.tags
}

resource "aws_cloudwatch_log_group" "mysql_slowquery" {
  name              = "/aws/rds/instance/${var.name_prefix}-mysql/slowquery"
  retention_in_days = 14

  tags = var.tags
}

resource "aws_db_instance" "this" {
  identifier = "${var.name_prefix}-mysql"

  engine                   = "mysql"
  engine_version           = var.engine_version
  engine_lifecycle_support = "open-source-rds-extended-support-disabled"
  instance_class           = var.instance_class

  db_name                     = var.database_name
  username                    = var.master_username
  manage_master_user_password = true
  port                        = 3306

  db_subnet_group_name   = aws_db_subnet_group.this.name
  vpc_security_group_ids = [var.db_security_group_id]
  parameter_group_name   = aws_db_parameter_group.this.name
  publicly_accessible    = false
  multi_az               = false

  storage_type          = "gp3"
  allocated_storage     = var.allocated_storage
  max_allocated_storage = var.max_allocated_storage
  storage_encrypted     = true

  backup_retention_period  = var.backup_retention_period
  backup_window            = "08:30-09:00"
  maintenance_window       = "sun:09:00-sun:09:30"
  copy_tags_to_snapshot    = true
  delete_automated_backups = false

  auto_minor_version_upgrade  = true
  allow_major_version_upgrade = false
  apply_immediately           = false

  deletion_protection       = true
  skip_final_snapshot       = false
  final_snapshot_identifier = "${var.name_prefix}-mysql-final"

  monitoring_interval                 = 0
  performance_insights_enabled        = false
  enabled_cloudwatch_logs_exports     = ["error", "slowquery"]
  iam_database_authentication_enabled = false

  tags = merge(var.tags, {
    Name = "${var.name_prefix}-mysql"
  })

  depends_on = [
    aws_cloudwatch_log_group.mysql_error,
    aws_cloudwatch_log_group.mysql_slowquery
  ]
}

resource "aws_secretsmanager_secret" "application" {
  name                    = "project-admin/dev/database/application"
  description             = "ProjectAdmin application database credentials. Secret value is registered outside Terraform."
  recovery_window_in_days = 7

  tags = var.tags
}

data "aws_iam_policy_document" "application_secret_read" {
  statement {
    sid    = "ReadApplicationDatabaseSecret"
    effect = "Allow"
    actions = [
      "secretsmanager:DescribeSecret",
      "secretsmanager:GetSecretValue"
    ]
    resources = [aws_secretsmanager_secret.application.arn]
  }
}

resource "aws_iam_role_policy" "application_secret_read" {
  name   = "${var.name_prefix}-database-secret-read"
  role   = var.application_role_name
  policy = data.aws_iam_policy_document.application_secret_read.json
}
