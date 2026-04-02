resource "aws_ssm_parameter" "db_password" {
  name  = "/${var.project_name}/db-password"
  type  = "SecureString"
  value = var.db_password

  tags = {
    Name = "${var.project_name}-db-password"
  }
}

resource "aws_ssm_parameter" "jwt_secret" {
  name  = "/${var.project_name}/jwt-secret"
  type  = "SecureString"
  value = var.jwt_secret

  tags = {
    Name = "${var.project_name}-jwt-secret"
  }
}

resource "aws_ssm_parameter" "groq_api_key" {
  name  = "/${var.project_name}/groq-api-key"
  type  = "SecureString"
  value = var.groq_api_key

  tags = {
    Name = "${var.project_name}-groq-api-key"
  }
}

resource "aws_ssm_parameter" "google_client_id" {
  name  = "/${var.project_name}/google-client-id"
  type  = "SecureString"
  value = var.google_client_id

  tags = {
    Name = "${var.project_name}-google-client-id"
  }
}

resource "aws_ssm_parameter" "google_client_secret" {
  name  = "/${var.project_name}/google-client-secret"
  type  = "SecureString"
  value = var.google_client_secret

  tags = {
    Name = "${var.project_name}-google-client-secret"
  }
}
