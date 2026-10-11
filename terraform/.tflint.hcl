# tflint configuration for the AWS slice. Run locally with
#   docker run --rm -v "$PWD/terraform:/data" -w /data ghcr.io/terraform-linters/tflint:v0.53.0 --init
#   docker run --rm -v "$PWD/terraform:/data" -w /data ghcr.io/terraform-linters/tflint:v0.53.0
# (same pins as .github/workflows/terraform.yml).

config {
  call_module_type = "local"
}

plugin "terraform" {
  enabled = true
  preset  = "recommended"
}

plugin "aws" {
  enabled = true
  version = "0.33.0"
  source  = "github.com/terraform-linters/tflint-ruleset-aws"
}
