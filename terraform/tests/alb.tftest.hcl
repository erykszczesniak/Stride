# The listener choice is explicit: with a domain the HTTP listener redirects
# to HTTPS and a TLS 1.3 listener forwards; without one, HTTP forwards and
# there is no HTTPS listener (no certificate to attach).
mock_provider "aws" {
  mock_data "aws_availability_zones" {
    defaults = {
      names = ["eu-central-1a", "eu-central-1b"]
    }
  }
}

variables {
  db_password = "test-only-password-1234"
  jwt_secret  = "test-only-jwt-secret-at-least-32-chars"
}

run "with_a_domain_http_redirects_to_https" {
  command = plan

  variables {
    domain_name = "stride.example.com"
  }

  assert {
    condition     = aws_lb_listener.http.default_action[0].type == "redirect"
    error_message = "With a domain the HTTP listener must redirect."
  }

  assert {
    condition     = aws_lb_listener.http.default_action[0].redirect[0].protocol == "HTTPS" && aws_lb_listener.http.default_action[0].redirect[0].port == "443" && aws_lb_listener.http.default_action[0].redirect[0].status_code == "HTTP_301"
    error_message = "The redirect must go to HTTPS on 443 with a 301."
  }

  assert {
    condition     = length(aws_lb_listener.https) == 1 && aws_lb_listener.https[0].ssl_policy == "ELBSecurityPolicy-TLS13-1-2-2021-06"
    error_message = "With a domain there is exactly one HTTPS listener on the TLS 1.3 policy."
  }

  assert {
    condition     = length(aws_acm_certificate.main) == 1 && aws_acm_certificate.main[0].validation_method == "DNS"
    error_message = "With a domain an ACM certificate with DNS validation is requested."
  }
}

run "without_a_domain_http_forwards_and_there_is_no_https_listener" {
  command = plan

  assert {
    condition     = aws_lb_listener.http.default_action[0].type == "forward" && length(aws_lb_listener.http.default_action[0].redirect) == 0
    error_message = "Without a domain the HTTP listener forwards to the target group."
  }

  assert {
    condition     = length(aws_lb_listener.https) == 0 && length(aws_acm_certificate.main) == 0
    error_message = "Without a domain there must be no HTTPS listener and no certificate."
  }

  assert {
    condition     = aws_lb.main.drop_invalid_header_fields == true
    error_message = "The ALB must drop invalid header fields."
  }
}
