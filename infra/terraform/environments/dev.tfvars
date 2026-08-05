# =============================================================================
# Ambiente: DEV
# Sizing minimo / custo reduzido. Sem Multi-AZ, NAT unico.
# Uso: terraform apply -var-file="environments/dev.tfvars"
# =============================================================================

environment = "dev"
aws_region  = "sa-east-1"

# --- Rede ---
vpc_cidr           = "10.20.0.0/16"
single_nat_gateway = true

# --- Containers ---
container_platform    = "ecs"
fargate_cpu           = 512
fargate_memory        = 1024
backend_desired_count = 1

# --- RDS ---
db_instance_class      = "db.t3.medium"
db_allocated_storage   = 20
db_multi_az            = false
db_deletion_protection = false

# --- Redis (no unico, sem failover em dev) ---
redis_node_type       = "cache.t3.micro"
redis_num_cache_nodes = 1

# --- Amazon MQ ---
mq_host_instance_type = "mq.t3.micro"
mq_deployment_mode    = "SINGLE_INSTANCE"

tags = {
  CostCenter = "ponto-dev"
}
