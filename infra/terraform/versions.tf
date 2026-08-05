# =============================================================================
# versions.tf
# Versoes de Terraform e providers para o projeto "Ponto Municipal".
# Regiao alvo: Brasil (AWS sa-east-1 / Sao Paulo).
# =============================================================================

terraform {
  required_version = ">= 1.5"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }

    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }

  # ---------------------------------------------------------------------------
  # Backend remoto de state (S3 + DynamoDB para lock).
  # Os valores ficam comentados de proposito: configure via `terraform init`
  # com -backend-config (ver README.md) para nao versionar nomes especificos
  # de bucket/tabela do cliente.
  #
  # Exemplo de uso:
  #   terraform init \
  #     -backend-config="bucket=ponto-municipal-tfstate" \
  #     -backend-config="key=ponto/terraform.tfstate" \
  #     -backend-config="region=sa-east-1" \
  #     -backend-config="dynamodb_table=ponto-municipal-tflock" \
  #     -backend-config="encrypt=true"
  # ---------------------------------------------------------------------------
  backend "s3" {
    # bucket         = "ponto-municipal-tfstate"
    # key            = "ponto/terraform.tfstate"
    # region         = "sa-east-1"
    # dynamodb_table = "ponto-municipal-tflock"
    # encrypt        = true
  }
}

# -----------------------------------------------------------------------------
# Provider AWS. As credenciais SAO DO CLIENTE e devem ser fornecidas via:
#   - variaveis de ambiente AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY, ou
#   - perfil nomeado (~/.aws/credentials) via AWS_PROFILE, ou
#   - IAM Role / SSO.
# NUNCA coloque credenciais neste arquivo.
# -----------------------------------------------------------------------------
provider "aws" {
  region = var.aws_region

  default_tags {
    tags = local.common_tags
  }
}
