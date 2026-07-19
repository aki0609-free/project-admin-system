provider "aws" {
  region = "ap-northeast-1"

  default_tags {
    tags = {
      Project     = "ProjectAdminSystem"
      Environment = "dev"
      ManagedBy   = "Terraform"
      Component   = "TerraformBootstrap"
    }
  }
}
