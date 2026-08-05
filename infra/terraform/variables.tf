# =============================================================================
# variables.tf
# Variaveis de entrada para a infraestrutura do "Ponto Municipal".
# Valores padrao apontam para um ambiente dev em sa-east-1 (Sao Paulo).
# Sobrescreva por ambiente via tfvars (dev/staging/prod) ou workspaces.
# =============================================================================

# ----------------------------------------------------------------------------
# Identificacao / regiao
# ----------------------------------------------------------------------------
variable "project_name" {
  description = "Nome do projeto, usado como prefixo nos recursos."
  type        = string
  default     = "ponto-municipal"
}

variable "aws_region" {
  description = "Regiao AWS. Padrao Brasil (Sao Paulo)."
  type        = string
  default     = "sa-east-1"
}

variable "environment" {
  description = "Ambiente alvo: dev, staging ou prod."
  type        = string
  default     = "dev"

  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "environment deve ser um de: dev, staging, prod."
  }
}

# ----------------------------------------------------------------------------
# Rede / VPC
# ----------------------------------------------------------------------------
variable "vpc_cidr" {
  description = "CIDR principal da VPC."
  type        = string
  default     = "10.20.0.0/16"
}

variable "availability_zones" {
  description = "AZs a utilizar em sa-east-1 (Multi-AZ requer ao menos 2)."
  type        = list(string)
  default     = ["sa-east-1a", "sa-east-1b", "sa-east-1c"]
}

variable "public_subnet_cidrs" {
  description = "CIDRs das subnets publicas (uma por AZ)."
  type        = list(string)
  default     = ["10.20.0.0/20", "10.20.16.0/20", "10.20.32.0/20"]
}

variable "private_subnet_cidrs" {
  description = "CIDRs das subnets privadas (uma por AZ)."
  type        = list(string)
  default     = ["10.20.64.0/20", "10.20.80.0/20", "10.20.96.0/20"]
}

variable "single_nat_gateway" {
  description = "Se true, usa um unico NAT Gateway (mais barato; menos resiliente). Recomendado false em prod."
  type        = bool
  default     = true
}

# ----------------------------------------------------------------------------
# Plataforma de containers
# ----------------------------------------------------------------------------
variable "container_platform" {
  description = "Plataforma de orquestracao: 'ecs' (Fargate) ou 'eks'."
  type        = string
  default     = "ecs"

  validation {
    condition     = contains(["ecs", "eks"], var.container_platform)
    error_message = "container_platform deve ser 'ecs' ou 'eks'."
  }
}

variable "eks_cluster_version" {
  description = "Versao do Kubernetes para o EKS (quando container_platform = eks)."
  type        = string
  default     = "1.30"
}

variable "eks_node_instance_types" {
  description = "Tipos de instancia para os node groups do EKS."
  type        = list(string)
  default     = ["t3.large"]
}

variable "eks_node_desired_size" {
  description = "Quantidade desejada de nodes do EKS."
  type        = number
  default     = 2
}

variable "fargate_cpu" {
  description = "CPU (unidades) da task ECS Fargate do backend."
  type        = number
  default     = 512
}

variable "fargate_memory" {
  description = "Memoria (MiB) da task ECS Fargate do backend."
  type        = number
  default     = 1024
}

variable "backend_container_image" {
  description = "Imagem do backend (caso vazio, usa a do ECR criado). Ex.: <conta>.dkr.ecr.sa-east-1.amazonaws.com/ponto-backend:latest"
  type        = string
  default     = ""
}

variable "backend_desired_count" {
  description = "Numero desejado de tasks do backend (ECS)."
  type        = number
  default     = 2
}

variable "web_container_image" {
  description = "Imagem do painel web (caso vazio, usa a do ECR criado)."
  type        = string
  default     = ""
}

variable "web_desired_count" {
  description = "Numero desejado de tasks do painel web (ECS)."
  type        = number
  default     = 2
}

# ----------------------------------------------------------------------------
# Autenticacao (Keycloak) e TLS
# ----------------------------------------------------------------------------
variable "keycloak_jwks_uri" {
  description = "URL do JWKS do realm do Keycloak (issuer + /protocol/openid-connect/certs). O backend valida os tokens dos admins por aqui."
  type        = string
  default     = ""
}

variable "acm_certificate_arn" {
  description = "ARN do certificado ACM para o listener HTTPS (443) do ALB. Vazio = so HTTP (NAO recomendado em prod)."
  type        = string
  default     = ""
}

# ----------------------------------------------------------------------------
# RDS PostgreSQL
# ----------------------------------------------------------------------------
variable "db_engine_version" {
  description = "Versao do PostgreSQL no RDS."
  type        = string
  default     = "16.4"
}

variable "db_instance_class" {
  description = "Classe de instancia do RDS."
  type        = string
  default     = "db.t3.medium"
}

variable "db_allocated_storage" {
  description = "Armazenamento inicial em GB."
  type        = number
  default     = 50
}

variable "db_max_allocated_storage" {
  description = "Teto de autoscaling de armazenamento em GB."
  type        = number
  default     = 200
}

variable "db_multi_az" {
  description = "Habilita Multi-AZ (alta disponibilidade). Recomendado true em staging/prod."
  type        = bool
  default     = true
}

variable "db_name" {
  description = "Nome do banco inicial."
  type        = string
  default     = "ponto"
}

variable "db_username" {
  description = "Usuario master/dono do RDS (roda as migrations Flyway)."
  type        = string
  default     = "ponto_admin"
}

variable "db_app_username" {
  description = "Usuario de RUNTIME da aplicacao (NAO-superusuario) — a RLS por tenant so vale para ele. Precisa ser criado no RDS (ver infra/postgres/10-create-app-role.sh)."
  type        = string
  default     = "ponto_app"
}

variable "db_backup_retention_days" {
  description = "Dias de retencao de backups automaticos."
  type        = number
  default     = 7
}

variable "db_deletion_protection" {
  description = "Protecao contra delecao do RDS. Recomendado true em prod."
  type        = bool
  default     = false
}

# ----------------------------------------------------------------------------
# ElastiCache Redis
# ----------------------------------------------------------------------------
variable "redis_engine_version" {
  description = "Versao do Redis."
  type        = string
  default     = "7.1"
}

variable "redis_node_type" {
  description = "Tipo de node do ElastiCache Redis."
  type        = string
  default     = "cache.t3.micro"
}

variable "redis_num_cache_nodes" {
  description = "Quantidade de nodes/replicas do Redis (>=2 para failover automatico)."
  type        = number
  default     = 2
}

# ----------------------------------------------------------------------------
# Amazon MQ (RabbitMQ)
# ----------------------------------------------------------------------------
variable "mq_engine_version" {
  description = "Versao do RabbitMQ no Amazon MQ."
  type        = string
  default     = "3.13"
}

variable "mq_host_instance_type" {
  description = "Tipo de instancia do broker Amazon MQ."
  type        = string
  default     = "mq.t3.micro"
}

variable "mq_deployment_mode" {
  description = "Modo de deploy do Amazon MQ: SINGLE_INSTANCE ou CLUSTER_MULTI_AZ."
  type        = string
  default     = "SINGLE_INSTANCE"

  validation {
    condition     = contains(["SINGLE_INSTANCE", "CLUSTER_MULTI_AZ"], var.mq_deployment_mode)
    error_message = "mq_deployment_mode deve ser 'SINGLE_INSTANCE' ou 'CLUSTER_MULTI_AZ'."
  }
}

variable "mq_username" {
  description = "Usuario do RabbitMQ (Amazon MQ)."
  type        = string
  default     = "ponto_mq"
}

# ----------------------------------------------------------------------------
# Tags
# ----------------------------------------------------------------------------
variable "tags" {
  description = "Tags adicionais aplicadas a todos os recursos."
  type        = map(string)
  default     = {}
}
