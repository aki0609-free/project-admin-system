provider "aws" {
  region = "ap-northeast-1"

  default_tags {
    tags = {
      Project     = "ProjectAdminSystem"
      Environment = "dev"
      ManagedBy   = "Terraform"
    }
  }
}

# Authentication is supplied only through the CLOUDFLARE_API_TOKEN environment
# variable. Never place the token in Terraform configuration or tfvars files.
provider "cloudflare" {}
