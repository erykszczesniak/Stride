output "rds_endpoint" {
  description = "RDS Postgres endpoint for stride_user — use as POSTGRES_HOST in user-service config."
  value       = aws_db_instance.user.address
}

output "rds_port" {
  description = "RDS port (5432)."
  value       = aws_db_instance.user.port
}

output "ec2_public_dns" {
  description = "Public DNS of the demo EC2 — visit on port 80 (nginx → gateway)."
  value       = aws_instance.demo.public_dns
}

output "s3_uploads_bucket" {
  description = "S3 bucket for activity uploads / photos."
  value       = aws_s3_bucket.uploads.bucket
}

output "ecr_repositories" {
  description = "Push docker images to these registries."
  value       = { for k, repo in aws_ecr_repository.service : k => repo.repository_url }
}
