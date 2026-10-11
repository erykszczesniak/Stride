# Guard rails on the network and data path. `mock_provider` stands in for
# AWS, so this runs in CI without credentials; computed attributes are made up,
# everything declared in the .tf files is real.
mock_provider "aws" {
  mock_data "aws_availability_zones" {
    defaults = {
      names = ["eu-central-1a", "eu-central-1b"]
    }
  }

  # The provider validates ARN syntax even against a mock, so the ARNs that
  # other resources reference need a plausible shape.
  mock_resource "aws_lb" {
    defaults = {
      arn = "arn:aws:elasticloadbalancing:eu-central-1:123456789012:loadbalancer/app/mock/0123456789abcdef"
    }
  }
  mock_resource "aws_lb_target_group" {
    defaults = {
      arn = "arn:aws:elasticloadbalancing:eu-central-1:123456789012:targetgroup/mock/0123456789abcdef"
    }
  }
  mock_resource "aws_acm_certificate" {
    defaults = {
      arn = "arn:aws:acm:eu-central-1:123456789012:certificate/00000000-0000-0000-0000-000000000000"
    }
  }
  mock_resource "aws_iam_role" {
    defaults = {
      arn = "arn:aws:iam::123456789012:role/mock"
    }
  }
}

variables {
  db_password = "test-only-password-1234"
  jwt_secret  = "test-only-jwt-secret-at-least-32-chars"
}

run "no_ssh_or_database_port_from_the_internet" {
  # apply (against the mock) because the egress set of a security group is
  # only known after apply; nothing is created anywhere.
  command = apply

  assert {
    condition = alltrue(flatten([
      for sg in [aws_security_group.alb, aws_security_group.ec2, aws_security_group.rds] : [
        for rule in sg.ingress : !(rule.from_port <= 22 && rule.to_port >= 22)
      ]
    ]))
    error_message = "No security group may open port 22; the host is reached through SSM Session Manager."
  }

  assert {
    condition = alltrue(flatten([
      for sg in [aws_security_group.alb, aws_security_group.ec2, aws_security_group.rds] : [
        for rule in sg.ingress : !(rule.from_port <= 5432 && rule.to_port >= 5432 && contains(coalesce(rule.cidr_blocks, []), "0.0.0.0/0"))
      ]
    ]))
    error_message = "The PostgreSQL port must never be open to 0.0.0.0/0."
  }

  assert {
    condition     = alltrue([for rule in aws_security_group.rds.ingress : length(coalesce(rule.cidr_blocks, [])) == 0 && length(rule.security_groups) == 1])
    error_message = "RDS accepts connections only from the EC2 security group, never from a CIDR."
  }

  assert {
    condition     = alltrue([for rule in aws_security_group.ec2.ingress : length(coalesce(rule.cidr_blocks, [])) == 0])
    error_message = "The EC2 host accepts traffic only from the ALB security group, never from a CIDR."
  }

  assert {
    condition     = alltrue([for rule in aws_security_group.alb.ingress : contains([80, 443], rule.from_port) && rule.from_port == rule.to_port])
    error_message = "The ALB exposes only 80 and 443 to the internet."
  }

  assert {
    condition     = length(aws_security_group.rds.egress) == 0
    error_message = "RDS never initiates connections; its security group must have no egress rule."
  }
}

run "database_is_private_encrypted_and_backed_up" {
  command = plan

  assert {
    condition     = aws_db_instance.postgres.publicly_accessible == false
    error_message = "RDS must not be publicly accessible."
  }

  assert {
    condition     = aws_db_instance.postgres.storage_encrypted == true
    error_message = "RDS storage must be encrypted at rest."
  }

  assert {
    condition     = aws_db_instance.postgres.backup_retention_period >= 7 && aws_db_instance.postgres.skip_final_snapshot == false
    error_message = "RDS keeps at least 7 days of backups and takes a final snapshot on destroy."
  }
}

run "host_hardening" {
  command = plan

  assert {
    condition     = aws_instance.app.metadata_options[0].http_tokens == "required"
    error_message = "The EC2 host must require IMDSv2 tokens."
  }

  assert {
    condition     = aws_instance.app.root_block_device[0].encrypted == true
    error_message = "The EC2 root volume must be encrypted."
  }
}

run "container_registry_is_immutable_and_scanned" {
  command = plan

  assert {
    condition = alltrue([
      for repo in [aws_ecr_repository.backend, aws_ecr_repository.frontend] :
      repo.image_tag_mutability == "IMMUTABLE" && repo.image_scanning_configuration[0].scan_on_push == true
    ])
    error_message = "Both ECR repositories must use immutable tags and scan on push."
  }
}
