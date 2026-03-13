output "alb_dns" {
  description = "ALB DNS name — access the app here"
  value       = aws_lb.main.dns_name
}

output "ec2_public_ip" {
  description = "EC2 instance public IP"
  value       = aws_instance.app.public_ip
}

output "rds_endpoint" {
  description = "RDS PostgreSQL endpoint"
  value       = aws_db_instance.postgres.endpoint
}

output "ecr_backend_url" {
  description = "ECR repository URL for backend"
  value       = aws_ecr_repository.backend.repository_url
}

output "ecr_frontend_url" {
  description = "ECR repository URL for frontend"
  value       = aws_ecr_repository.frontend.repository_url
}

output "push_backend_image" {
  description = "Command to build and push backend image"
  value       = <<-EOT
    aws ecr get-login-password --region ${var.aws_region} | docker login --username AWS --password-stdin ${aws_ecr_repository.backend.repository_url}
    docker build -t ${aws_ecr_repository.backend.repository_url}:${var.backend_image_tag} .
    docker push ${aws_ecr_repository.backend.repository_url}:${var.backend_image_tag}
  EOT
}

output "push_frontend_image" {
  description = "Command to build and push frontend image"
  value       = <<-EOT
    aws ecr get-login-password --region ${var.aws_region} | docker login --username AWS --password-stdin ${aws_ecr_repository.frontend.repository_url}
    docker build -t ${aws_ecr_repository.frontend.repository_url}:${var.frontend_image_tag} ./frontend
    docker push ${aws_ecr_repository.frontend.repository_url}:${var.frontend_image_tag}
  EOT
}
