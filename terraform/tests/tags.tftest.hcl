# Every taggable resource carries Project and Environment from variables, so
# cost reports and the console can be filtered per deployment. Add a line here
# when adding a taggable resource; the list is explicit on purpose.
mock_provider "aws" {
  mock_data "aws_availability_zones" {
    defaults = {
      names = ["eu-central-1a", "eu-central-1b"]
    }
  }
}

variables {
  db_password  = "test-only-password-1234"
  jwt_secret   = "test-only-jwt-secret-at-least-32-chars"
  project_name = "stride-test"
  environment  = "staging"
  domain_name  = "stride.example.com" # so the optional certificate and HTTPS listener exist too
}

run "every_taggable_resource_carries_project_and_environment" {
  command = plan

  assert {
    condition = alltrue([
      for tags in concat(
        [
          aws_vpc.main.tags,
          aws_internet_gateway.main.tags,
          aws_route_table.public.tags,
          aws_security_group.alb.tags,
          aws_security_group.ec2.tags,
          aws_security_group.rds.tags,
          aws_lb.main.tags,
          aws_lb_target_group.app.tags,
          aws_lb_listener.http.tags,
          aws_lb_listener.https[0].tags,
          aws_acm_certificate.main[0].tags,
          aws_instance.app.tags,
          aws_db_subnet_group.main.tags,
          aws_db_instance.postgres.tags,
          aws_ecr_repository.backend.tags,
          aws_ecr_repository.frontend.tags,
          aws_iam_role.ec2.tags,
          aws_iam_instance_profile.ec2.tags,
        ],
        [for s in aws_subnet.public : s.tags],
        [for p in [
          aws_ssm_parameter.db_password,
          aws_ssm_parameter.jwt_secret,
          aws_ssm_parameter.groq_api_key,
          aws_ssm_parameter.google_client_id,
          aws_ssm_parameter.google_client_secret,
        ] : p.tags],
      ) :
      lookup(tags, "Project", "") == "stride-test" && lookup(tags, "Environment", "") == "staging" && lookup(tags, "Name", "") != ""
    ])
    error_message = "A taggable resource is missing Project, Environment or Name (see locals.tf)."
  }
}

run "environment_is_validated" {
  command = plan

  variables {
    environment = "production"
  }

  expect_failures = [var.environment]
}
