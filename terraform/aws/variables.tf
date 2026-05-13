variable "aws_region" {
  description = "AWS region for the demo slice. eu-central-1 (Frankfurt) is closer for European users than the default us-east-1."
  type        = string
  default     = "eu-central-1"
}

variable "environment" {
  description = "Environment slug — used to namespace resource names (dev / staging / prod)."
  type        = string
  default     = "dev"
}

variable "db_password" {
  description = "RDS master password. Pass via TF_VAR_db_password env var or .tfvars file; do not commit."
  type        = string
  sensitive   = true
}

variable "jwt_secret" {
  description = "HS256 JWT signing secret, min 32 chars. Same secret used by gateway + user-service."
  type        = string
  sensitive   = true
}

variable "groq_api_key" {
  description = "Groq API key for ai-service. Empty string is acceptable (falls back to canned response)."
  type        = string
  sensitive   = true
  default     = ""
}

variable "ssh_cidr_blocks" {
  description = "CIDR blocks allowed to SSH the demo EC2. Default open — TIGHTEN before any real use."
  type        = list(string)
  default     = ["0.0.0.0/0"]
}
