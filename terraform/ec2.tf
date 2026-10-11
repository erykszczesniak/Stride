data "aws_ami" "amazon_linux" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-*-x86_64"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

resource "aws_instance" "app" {
  ami                    = data.aws_ami.amazon_linux.id
  instance_type          = var.ec2_instance_type
  key_name               = var.ec2_key_pair_name
  subnet_id              = aws_subnet.public[0].id
  vpc_security_group_ids = [aws_security_group.ec2.id]
  iam_instance_profile   = aws_iam_instance_profile.ec2.name

  user_data = templatefile("${path.module}/scripts/user-data.sh", {
    aws_region         = var.aws_region
    project_name       = var.project_name
    ecr_backend_url    = aws_ecr_repository.backend.repository_url
    ecr_frontend_url   = aws_ecr_repository.frontend.repository_url
    backend_image_tag  = var.backend_image_tag
    frontend_image_tag = var.frontend_image_tag
    db_host            = aws_db_instance.postgres.address
    db_port            = aws_db_instance.postgres.port
    db_name            = var.db_name
    db_username        = var.db_username
    cors_origins       = var.domain_name != "" ? "https://${var.domain_name}" : "http://${aws_lb.main.dns_name}"
  })

  root_block_device {
    volume_size = 20
    volume_type = "gp3"
    encrypted   = true # at rest, default EBS key (trivy AVD-AWS-0131)
  }

  # IMDSv2 only: a token is required to read instance metadata (trivy AVD-AWS-0028).
  # The AWS CLI in user-data.sh and the SSM agent both speak IMDSv2.
  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"
    http_put_response_hop_limit = 1
  }

  tags = merge(local.common_tags, {
    Name = "${var.project_name}-app"
  })

  depends_on = [
    aws_db_instance.postgres,
    aws_ssm_parameter.db_password,
    aws_ssm_parameter.jwt_secret,
  ]
}
