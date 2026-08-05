# =============================================================================
# outputs.tf
# Saidas uteis para integracao com pipelines, backend e operacao.
# Valores sensiveis (senhas) NAO sao expostos: leia os ARNs dos segredos.
# =============================================================================

# ----------------------------------------------------------------------------
# Rede
# ----------------------------------------------------------------------------
output "vpc_id" {
  description = "ID da VPC."
  value       = module.vpc.vpc_id
}

output "public_subnet_ids" {
  description = "IDs das subnets publicas."
  value       = module.vpc.public_subnets
}

output "private_subnet_ids" {
  description = "IDs das subnets privadas."
  value       = module.vpc.private_subnets
}

# ----------------------------------------------------------------------------
# RDS PostgreSQL
# ----------------------------------------------------------------------------
output "rds_endpoint" {
  description = "Endpoint (host) do RDS PostgreSQL."
  value       = aws_db_instance.postgres.address
}

output "rds_port" {
  description = "Porta do RDS PostgreSQL."
  value       = aws_db_instance.postgres.port
}

output "rds_secret_arn" {
  description = "ARN do segredo (Secrets Manager) com as credenciais do RDS."
  value       = aws_secretsmanager_secret.db.arn
}

# ----------------------------------------------------------------------------
# ElastiCache Redis
# ----------------------------------------------------------------------------
output "redis_primary_endpoint" {
  description = "Endpoint primario do Redis."
  value       = aws_elasticache_replication_group.redis.primary_endpoint_address
}

output "redis_reader_endpoint" {
  description = "Endpoint de leitura do Redis."
  value       = aws_elasticache_replication_group.redis.reader_endpoint_address
}

# ----------------------------------------------------------------------------
# Amazon MQ (RabbitMQ)
# ----------------------------------------------------------------------------
output "mq_console_url" {
  description = "URL do console web do RabbitMQ (Amazon MQ)."
  value       = aws_mq_broker.rabbitmq.instances[0].console_url
}

output "mq_amqp_endpoints" {
  description = "Endpoints AMQPS do broker RabbitMQ."
  value       = aws_mq_broker.rabbitmq.instances[0].endpoints
}

output "mq_secret_arn" {
  description = "ARN do segredo (Secrets Manager) com as credenciais do Amazon MQ."
  value       = aws_secretsmanager_secret.mq.arn
}

# ----------------------------------------------------------------------------
# S3
# ----------------------------------------------------------------------------
output "s3_buckets" {
  description = "Mapa de buckets S3 criados (fotos/atestados/afd)."
  value       = { for k, b in aws_s3_bucket.this : k => b.bucket }
}

# ----------------------------------------------------------------------------
# ECR
# ----------------------------------------------------------------------------
output "ecr_repository_urls" {
  description = "URLs dos repositorios ECR (backend/web)."
  value       = { for k, r in aws_ecr_repository.this : k => r.repository_url }
}

# ----------------------------------------------------------------------------
# Plataforma de containers
# ----------------------------------------------------------------------------
output "alb_dns_name" {
  description = "DNS publico do Application Load Balancer (ECS Fargate). Vazio se usar EKS."
  value       = local.use_ecs ? aws_lb.this[0].dns_name : ""
}

output "ecs_cluster_name" {
  description = "Nome do cluster ECS. Vazio se usar EKS."
  value       = local.use_ecs ? aws_ecs_cluster.this[0].name : ""
}

output "eks_cluster_name" {
  description = "Nome do cluster EKS. Vazio se usar ECS."
  value       = local.use_eks ? module.eks[0].cluster_name : ""
}

output "eks_cluster_endpoint" {
  description = "Endpoint da API do cluster EKS. Vazio se usar ECS."
  value       = local.use_eks ? module.eks[0].cluster_endpoint : ""
}

# ----------------------------------------------------------------------------
# Geral
# ----------------------------------------------------------------------------
output "region" {
  description = "Regiao AWS em uso."
  value       = var.aws_region
}

output "environment" {
  description = "Ambiente provisionado."
  value       = var.environment
}
