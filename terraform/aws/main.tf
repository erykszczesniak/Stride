###############################################################################
# Stride — minimal AWS deployment slice.
#
# What this provisions (all chosen to fit in 12-month Free Tier):
#
#   · RDS PostgreSQL db.t3.micro   — 750 h/mo + 20 GB storage, free
#   · S3 bucket                    — 5 GB free
#   · ECR private repo per service — 500 MB free
#   · EC2 t3.micro                 — 750 h/mo free for 12 months
#   · SES (sandbox)                — 62k emails/mo from EC2, free
#   · Parameter Store              — always free (used instead of Secrets Manager)
#
# What's deliberately NOT here:
#   · EKS — control plane is ~$73/mo, not free
#   · ALB — not free, use the t3.micro's nginx instead
#   · Secrets Manager — not free, Parameter Store SecureString is
#
# This is a SKELETON — not deployed automatically. Run from this directory:
#
#     terraform init
#     terraform plan -out=stride.tfplan
#     terraform apply stride.tfplan
#
###############################################################################

terraform {
  required_version = ">= 1.6"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
  default_tags {
    tags = {
      Project     = "stride"
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}

###############################################################################
# Networking — use the default VPC. For prod we'd want a custom VPC with
# private subnets and a NAT gateway, but that runs $32/mo for the NAT alone.
###############################################################################
data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

###############################################################################
# Security groups
###############################################################################
resource "aws_security_group" "demo_ec2" {
  name        = "stride-demo-ec2-${var.environment}"
  description = "Stride demo EC2 — allow HTTP + SSH from anywhere"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description = "HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = var.ssh_cidr_blocks
  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "rds" {
  name        = "stride-rds-${var.environment}"
  description = "Stride RDS — only EC2 can reach it"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.demo_ec2.id]
  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

###############################################################################
# RDS PostgreSQL — free tier db.t3.micro
###############################################################################
resource "aws_db_subnet_group" "stride" {
  name       = "stride-${var.environment}"
  subnet_ids = data.aws_subnets.default.ids
}

resource "aws_db_instance" "user" {
  identifier             = "stride-user-${var.environment}"
  engine                 = "postgres"
  engine_version         = "16.4"
  instance_class         = "db.t3.micro"
  allocated_storage      = 20
  storage_type           = "gp3"
  db_name                = "stride_user"
  username               = "stride"
  password               = var.db_password
  db_subnet_group_name   = aws_db_subnet_group.stride.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  skip_final_snapshot    = true
  publicly_accessible    = false
  backup_retention_period = 0   # free tier doesn't include backups
  deletion_protection    = false
}

###############################################################################
# S3 — activity uploads (GPX/TCX/FIT files, activity photos)
###############################################################################
resource "aws_s3_bucket" "uploads" {
  bucket = "stride-uploads-${var.environment}-${random_id.suffix.hex}"
}

resource "aws_s3_bucket_public_access_block" "uploads" {
  bucket                  = aws_s3_bucket.uploads.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "random_id" "suffix" {
  byte_length = 3
}

###############################################################################
# ECR — Docker image registry, one repo per service
###############################################################################
resource "aws_ecr_repository" "service" {
  for_each = toset([
    "config-server", "discovery-server", "api-gateway",
    "user-service", "activity-service", "notification-service",
    "social-service", "club-event-service", "challenge-goal-service", "ai-service"
  ])

  name                 = "stride/${each.key}"
  image_tag_mutability = "MUTABLE"
  image_scanning_configuration {
    scan_on_push = true
  }
}

###############################################################################
# EC2 t3.micro — runs gateway + 1-2 services in docker-compose as the
# "live demo slice" link in the portfolio README. Each Spring Boot JVM
# is capped at -Xmx256m so 3 of them fit in 1 GiB RAM.
###############################################################################
data "aws_ami" "al2023" {
  most_recent = true
  owners      = ["amazon"]
  filter {
    name   = "name"
    values = ["al2023-ami-2023.*-x86_64"]
  }
}

resource "aws_instance" "demo" {
  ami                    = data.aws_ami.al2023.id
  instance_type          = "t3.micro"
  vpc_security_group_ids = [aws_security_group.demo_ec2.id]
  user_data              = <<-EOF
    #!/bin/bash
    dnf install -y docker
    systemctl enable --now docker
    usermod -aG docker ec2-user
    curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64" \
      -o /usr/local/bin/docker-compose && chmod +x /usr/local/bin/docker-compose
  EOF
}

###############################################################################
# Parameter Store — used INSTEAD of Secrets Manager since SM costs $0.40/secret.
###############################################################################
resource "aws_ssm_parameter" "jwt_secret" {
  name  = "/stride/${var.environment}/JWT_SECRET"
  type  = "SecureString"
  value = var.jwt_secret
}

resource "aws_ssm_parameter" "db_password" {
  name  = "/stride/${var.environment}/POSTGRES_PASSWORD"
  type  = "SecureString"
  value = var.db_password
}

resource "aws_ssm_parameter" "groq_api_key" {
  name  = "/stride/${var.environment}/GROQ_API_KEY"
  type  = "SecureString"
  value = var.groq_api_key
}
