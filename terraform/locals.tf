# Tags every taggable resource carries, merged with the resource's own Name.
# terraform/tests/tags.tftest.hcl asserts that no resource misses them; cost
# attribution (docs/cost-attribution.md) groups by Project and Environment.
locals {
  common_tags = {
    Project     = var.project_name
    Environment = var.environment
    ManagedBy   = "terraform"
  }
}
