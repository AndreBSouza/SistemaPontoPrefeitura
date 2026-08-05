# =============================================================================
# Ambiente: PROD
# Alta disponibilidade: Multi-AZ em RDS/Redis/MQ, NAT por AZ, protecoes ligadas.
# Uso: terraform apply -var-file="environments/prod.tfvars"
# =============================================================================

environment = "prod"
aws_region  = "sa-east-1"

# --- Rede (NAT por AZ para resiliencia) ---
vpc_cidr           = "10.40.0.0/16"
single_nat_gateway = false

# --- Containers ---
# Multi-réplica OK: o rate-limit anti-abuso e o cache/revogação de token de dispositivo usam o
# Redis (SPRING_DATA_REDIS_HOST é injetado na task), então valem para o sistema todo, não por nó.
container_platform    = "ecs"
fargate_cpu           = 2048
fargate_memory        = 4096
backend_desired_count = 3

# --- RDS (Multi-AZ + protecao contra delecao) ---
db_instance_class        = "db.m6g.large"
db_allocated_storage     = 100
db_max_allocated_storage = 500
db_multi_az              = true
db_backup_retention_days = 30
db_deletion_protection   = true

# --- Redis (replicado, Multi-AZ) ---
redis_node_type       = "cache.m6g.large"
redis_num_cache_nodes = 2

# --- Amazon MQ (cluster Multi-AZ) ---
mq_host_instance_type = "mq.m5.large"
mq_deployment_mode    = "CLUSTER_MULTI_AZ"

tags = {
  CostCenter = "ponto-prod"
  Compliance = "ponto-eletronico-municipal"
}
