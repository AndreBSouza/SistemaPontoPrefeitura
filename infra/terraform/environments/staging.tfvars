# =============================================================================
# Ambiente: STAGING
# Aproxima-se de producao para homologacao. Multi-AZ ligado.
# Uso: terraform apply -var-file="environments/staging.tfvars"
# =============================================================================

environment = "staging"
aws_region  = "sa-east-1"

# --- Rede ---
vpc_cidr           = "10.30.0.0/16"
single_nat_gateway = true

# --- Containers ---
container_platform    = "ecs"
fargate_cpu           = 1024
fargate_memory        = 2048
backend_desired_count = 2

# --- RDS ---
db_instance_class      = "db.t3.large"
db_allocated_storage   = 50
db_multi_az            = true
db_deletion_protection = false

# --- Redis ---
redis_node_type       = "cache.t3.small"
redis_num_cache_nodes = 2

# --- Amazon MQ ---
mq_host_instance_type = "mq.t3.micro"
mq_deployment_mode    = "SINGLE_INSTANCE"

tags = {
  CostCenter = "ponto-staging"
}
