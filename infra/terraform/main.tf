# =============================================================================
# main.tf
# Infraestrutura do "Ponto Municipal" em AWS sa-east-1 (Sao Paulo).
#
# Esqueleto funcional e comentado. Cria:
#   - VPC com subnets publicas + privadas (Multi-AZ) via modulo oficial
#   - Plataforma de containers: ECS Fargate (padrao) OU EKS (opcional)
#   - RDS PostgreSQL Multi-AZ
#   - ElastiCache Redis (com failover)
#   - Amazon MQ RabbitMQ
#   - Buckets S3 (fotos, atestados, AFD) com versionamento + criptografia
#   - Repositorios ECR
#
# IMPORTANTE: credenciais sao do cliente (ver README.md). Segredos (senhas do
# RDS e do MQ) sao gerados e armazenados em AWS Secrets Manager.
# =============================================================================

# ----------------------------------------------------------------------------
# Locals: nomeacao e tags comuns
# ----------------------------------------------------------------------------
locals {
  name_prefix = "${var.project_name}-${var.environment}"

  common_tags = merge(
    {
      Project     = var.project_name
      Environment = var.environment
      ManagedBy   = "terraform"
      Region      = var.aws_region
      Country     = "BR"
    },
    var.tags
  )

  use_eks = var.container_platform == "eks"
  use_ecs = var.container_platform == "ecs"
}

# Dados da conta/regiao atual.
data "aws_caller_identity" "current" {}
data "aws_region" "current" {}

# =============================================================================
# 1) REDE - VPC, subnets publicas/privadas, NAT, IGW
#    Usa o modulo oficial terraform-aws-modules/vpc/aws.
# =============================================================================
module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "~> 5.0"

  name = "${local.name_prefix}-vpc"
  cidr = var.vpc_cidr

  azs             = var.availability_zones
  public_subnets  = var.public_subnet_cidrs
  private_subnets = var.private_subnet_cidrs

  enable_nat_gateway     = true
  single_nat_gateway     = var.single_nat_gateway
  one_nat_gateway_per_az = !var.single_nat_gateway

  enable_dns_hostnames = true
  enable_dns_support   = true

  # Tags exigidas pelo EKS para descoberta de subnets (inofensivas no ECS).
  public_subnet_tags = local.use_eks ? {
    "kubernetes.io/role/elb"                            = "1"
    "kubernetes.io/cluster/${local.name_prefix}-eks"    = "shared"
  } : {}

  private_subnet_tags = local.use_eks ? {
    "kubernetes.io/role/internal-elb"                   = "1"
    "kubernetes.io/cluster/${local.name_prefix}-eks"    = "shared"
  } : {}

  tags = local.common_tags
}

# =============================================================================
# 2) SECURITY GROUPS
# =============================================================================

# SG das tasks/pods da aplicacao (backend).
resource "aws_security_group" "app" {
  name        = "${local.name_prefix}-app-sg"
  description = "Tasks/pods do backend Ponto Municipal"
  vpc_id      = module.vpc.vpc_id

  egress {
    description = "Saida liberada"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(local.common_tags, { Name = "${local.name_prefix}-app-sg" })
}

# SG do ALB (entrada HTTP/HTTPS publica).
resource "aws_security_group" "alb" {
  count       = local.use_ecs ? 1 : 0
  name        = "${local.name_prefix}-alb-sg"
  description = "Application Load Balancer publico"
  vpc_id      = module.vpc.vpc_id

  ingress {
    description = "HTTPS"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTP (redireciona para HTTPS)"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(local.common_tags, { Name = "${local.name_prefix}-alb-sg" })
}

# Permite que o ALB alcance as tasks do backend (porta 8080).
resource "aws_security_group_rule" "app_from_alb" {
  count                    = local.use_ecs ? 1 : 0
  type                     = "ingress"
  from_port                = 8080
  to_port                  = 8080
  protocol                 = "tcp"
  security_group_id        = aws_security_group.app.id
  source_security_group_id = aws_security_group.alb[0].id
  description              = "Backend recebe trafego do ALB"
}

# Permite que o ALB alcance as tasks do painel web (porta 80).
resource "aws_security_group_rule" "web_from_alb" {
  count                    = local.use_ecs ? 1 : 0
  type                     = "ingress"
  from_port                = 80
  to_port                  = 80
  protocol                 = "tcp"
  security_group_id        = aws_security_group.app.id
  source_security_group_id = aws_security_group.alb[0].id
  description              = "Painel web recebe trafego do ALB"
}

# SG do RDS PostgreSQL - so aceita conexao das tasks da aplicacao.
resource "aws_security_group" "rds" {
  name        = "${local.name_prefix}-rds-sg"
  description = "RDS PostgreSQL"
  vpc_id      = module.vpc.vpc_id

  ingress {
    description     = "PostgreSQL a partir do backend"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.app.id]
  }

  tags = merge(local.common_tags, { Name = "${local.name_prefix}-rds-sg" })
}

# SG do Redis - so aceita conexao das tasks da aplicacao.
resource "aws_security_group" "redis" {
  name        = "${local.name_prefix}-redis-sg"
  description = "ElastiCache Redis"
  vpc_id      = module.vpc.vpc_id

  ingress {
    description     = "Redis a partir do backend"
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [aws_security_group.app.id]
  }

  tags = merge(local.common_tags, { Name = "${local.name_prefix}-redis-sg" })
}

# SG do Amazon MQ (RabbitMQ) - AMQPS (5671) a partir do backend.
resource "aws_security_group" "mq" {
  name        = "${local.name_prefix}-mq-sg"
  description = "Amazon MQ RabbitMQ"
  vpc_id      = module.vpc.vpc_id

  ingress {
    description     = "AMQPS a partir do backend"
    from_port       = 5671
    to_port         = 5671
    protocol        = "tcp"
    security_groups = [aws_security_group.app.id]
  }

  tags = merge(local.common_tags, { Name = "${local.name_prefix}-mq-sg" })
}

# =============================================================================
# 3) SEGREDOS - senhas aleatorias guardadas no Secrets Manager
# =============================================================================
resource "random_password" "db" {
  length  = 24
  special = true
  # Caracteres incompativeis com o RDS removidos.
  override_special = "!#$%&*()-_=+[]{}"
}

resource "random_password" "mq" {
  length           = 24
  special          = true
  override_special = "!#$%&*()-_=+[]{}"
}

resource "aws_secretsmanager_secret" "db" {
  name        = "${local.name_prefix}/rds/master"
  description = "Credenciais master do RDS PostgreSQL do Ponto Municipal"
  tags        = local.common_tags
}

resource "aws_secretsmanager_secret_version" "db" {
  secret_id = aws_secretsmanager_secret.db.id
  secret_string = jsonencode({
    username = var.db_username
    password = random_password.db.result
    engine   = "postgres"
    host     = aws_db_instance.postgres.address
    port     = 5432
    dbname   = var.db_name
  })
}

resource "aws_secretsmanager_secret" "mq" {
  name        = "${local.name_prefix}/mq/admin"
  description = "Credenciais do Amazon MQ RabbitMQ do Ponto Municipal"
  tags        = local.common_tags
}

resource "aws_secretsmanager_secret_version" "mq" {
  secret_id = aws_secretsmanager_secret.mq.id
  secret_string = jsonencode({
    username = var.mq_username
    password = random_password.mq.result
  })
}

# Segredo de criptografia em repouso (PONTO_CRYPTO_SECRET) — cifra o template biometrico (LGPD).
resource "random_password" "crypto" {
  length           = 48
  special          = true
  override_special = "!#$%&*()-_=+"
}

resource "aws_secretsmanager_secret" "crypto" {
  name        = "${local.name_prefix}/app/crypto"
  description = "PONTO_CRYPTO_SECRET (criptografia em repouso — LGPD) do Ponto Municipal"
  tags        = local.common_tags
}

resource "aws_secretsmanager_secret_version" "crypto" {
  secret_id     = aws_secretsmanager_secret.crypto.id
  secret_string = random_password.crypto.result
}

# Senha do role de RUNTIME da aplicacao (nao-superusuario, sujeito a RLS). O role em si precisa
# ser criado no RDS com ESTA senha — rode o SQL de infra/postgres/10-create-app-role.sh no banco
# (ex.: via bastion/psql) usando os valores deste segredo. O master (db) roda o Flyway.
resource "random_password" "db_app" {
  length           = 24
  special          = true
  override_special = "!#$%&*()-_=+[]{}"
}

resource "aws_secretsmanager_secret" "db_app" {
  name        = "${local.name_prefix}/rds/app"
  description = "Credenciais do role de aplicacao (runtime, RLS) do RDS do Ponto Municipal"
  tags        = local.common_tags
}

resource "aws_secretsmanager_secret_version" "db_app" {
  secret_id = aws_secretsmanager_secret.db_app.id
  secret_string = jsonencode({
    username = var.db_app_username
    password = random_password.db_app.result
  })
}

# =============================================================================
# 4) RDS PostgreSQL (Multi-AZ)
# =============================================================================
resource "aws_db_subnet_group" "postgres" {
  name       = "${local.name_prefix}-rds-subnets"
  subnet_ids = module.vpc.private_subnets
  tags       = local.common_tags
}

resource "aws_db_instance" "postgres" {
  identifier     = "${local.name_prefix}-pg"
  engine         = "postgres"
  engine_version = var.db_engine_version
  instance_class = var.db_instance_class

  allocated_storage     = var.db_allocated_storage
  max_allocated_storage = var.db_max_allocated_storage
  storage_type          = "gp3"
  storage_encrypted     = true # criptografia em repouso (KMS gerenciado pela AWS)

  db_name  = var.db_name
  username = var.db_username
  password = random_password.db.result
  port     = 5432

  multi_az               = var.db_multi_az
  db_subnet_group_name   = aws_db_subnet_group.postgres.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  publicly_accessible    = false

  backup_retention_period = var.db_backup_retention_days
  backup_window           = "03:00-04:00" # horario de baixo uso (UTC)
  maintenance_window      = "Mon:04:00-Mon:05:00"

  deletion_protection       = var.db_deletion_protection
  skip_final_snapshot       = var.environment != "prod"
  final_snapshot_identifier = var.environment == "prod" ? "${local.name_prefix}-pg-final" : null

  performance_insights_enabled = true
  auto_minor_version_upgrade   = true

  tags = merge(local.common_tags, { Name = "${local.name_prefix}-pg" })
}

# =============================================================================
# 5) ElastiCache Redis (replication group com failover)
# =============================================================================
resource "aws_elasticache_subnet_group" "redis" {
  name       = "${local.name_prefix}-redis-subnets"
  subnet_ids = module.vpc.private_subnets
  tags       = local.common_tags
}

resource "aws_elasticache_replication_group" "redis" {
  replication_group_id = "${local.name_prefix}-redis"
  description          = "Redis do Ponto Municipal (cache/sessao/rate-limit)"

  engine         = "redis"
  engine_version = var.redis_engine_version
  node_type      = var.redis_node_type
  port           = 6379

  num_cache_clusters         = var.redis_num_cache_nodes
  automatic_failover_enabled = var.redis_num_cache_nodes > 1
  multi_az_enabled           = var.redis_num_cache_nodes > 1

  subnet_group_name  = aws_elasticache_subnet_group.redis.name
  security_group_ids = [aws_security_group.redis.id]

  at_rest_encryption_enabled = true
  transit_encryption_enabled = true

  snapshot_retention_limit = var.environment == "prod" ? 5 : 1

  tags = merge(local.common_tags, { Name = "${local.name_prefix}-redis" })
}

# =============================================================================
# 6) Amazon MQ - RabbitMQ
# =============================================================================
resource "aws_mq_broker" "rabbitmq" {
  broker_name = "${local.name_prefix}-rabbitmq"

  engine_type        = "RabbitMQ"
  engine_version     = var.mq_engine_version
  host_instance_type = var.mq_host_instance_type
  deployment_mode    = var.mq_deployment_mode

  # Broker privado, dentro das subnets privadas.
  # CLUSTER_MULTI_AZ exige multiplas subnets; SINGLE_INSTANCE usa uma.
  subnet_ids = var.mq_deployment_mode == "CLUSTER_MULTI_AZ" ? slice(module.vpc.private_subnets, 0, 2) : [module.vpc.private_subnets[0]]

  security_groups            = [aws_security_group.mq.id]
  publicly_accessible        = false
  auto_minor_version_upgrade = true

  user {
    username = var.mq_username
    password = random_password.mq.result
  }

  logs {
    general = true
  }

  tags = merge(local.common_tags, { Name = "${local.name_prefix}-rabbitmq" })
}

# =============================================================================
# 7) S3 - buckets para fotos, atestados e AFD (Arquivo Fonte de Dados / ponto)
#    Todos com versionamento, criptografia e bloqueio de acesso publico.
# =============================================================================
locals {
  # Sufixo unico por conta/regiao para garantir nomes globais unicos.
  bucket_suffix = "${data.aws_caller_identity.current.account_id}-${var.aws_region}"

  s3_buckets = {
    fotos     = "${local.name_prefix}-fotos-${local.bucket_suffix}"
    atestados = "${local.name_prefix}-atestados-${local.bucket_suffix}"
    afd       = "${local.name_prefix}-afd-${local.bucket_suffix}"
  }
}

resource "aws_s3_bucket" "this" {
  for_each = local.s3_buckets

  bucket = each.value
  tags   = merge(local.common_tags, { Name = each.value, Purpose = each.key })
}

resource "aws_s3_bucket_versioning" "this" {
  for_each = aws_s3_bucket.this

  bucket = each.value.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "this" {
  for_each = aws_s3_bucket.this

  bucket = each.value.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "aws:kms" # criptografia com KMS
    }
    bucket_key_enabled = true
  }
}

resource "aws_s3_bucket_public_access_block" "this" {
  for_each = aws_s3_bucket.this

  bucket                  = each.value.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Ciclo de vida: AFD e atestados sao documentos legais; mantemos versoes
# antigas por mais tempo e movemos para classe mais barata apos 90 dias.
resource "aws_s3_bucket_lifecycle_configuration" "this" {
  for_each = aws_s3_bucket.this

  bucket = each.value.id

  rule {
    id     = "transicao-versoes-antigas"
    status = "Enabled"

    noncurrent_version_transition {
      noncurrent_days = 90
      storage_class   = "STANDARD_IA"
    }

    noncurrent_version_expiration {
      noncurrent_days = 365
    }
  }
}

# =============================================================================
# 8) ECR - repositorios de imagens (backend + web)
# =============================================================================
locals {
  ecr_repos = {
    backend = "${var.project_name}/backend"
    web     = "${var.project_name}/web"
  }
}

resource "aws_ecr_repository" "this" {
  for_each = local.ecr_repos

  name                 = each.value
  image_tag_mutability = "IMMUTABLE"
  force_delete         = var.environment != "prod"

  image_scanning_configuration {
    scan_on_push = true
  }

  encryption_configuration {
    encryption_type = "AES256"
  }

  tags = merge(local.common_tags, { Name = each.value })
}

# Mantem apenas as ultimas 20 imagens por repositorio.
resource "aws_ecr_lifecycle_policy" "this" {
  for_each = aws_ecr_repository.this

  repository = each.value.name
  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Manter apenas as ultimas 20 imagens"
        selection = {
          tagStatus   = "any"
          countType   = "imageCountMoreThan"
          countNumber = 20
        }
        action = { type = "expire" }
      }
    ]
  })
}

# =============================================================================
# 9) PLATAFORMA DE CONTAINERS
# =============================================================================

# ----------------------------------------------------------------------------
# 9a) ECS Fargate (padrao). Inclui cluster, ALB, task definition e service.
#     Logs no CloudWatch. Roles de execucao/task com acesso a S3 e Secrets.
# ----------------------------------------------------------------------------

# --- IAM: execution role (puxar imagem do ECR, escrever logs, ler segredos) ---
data "aws_iam_policy_document" "ecs_assume" {
  count = local.use_ecs ? 1 : 0
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "ecs_execution" {
  count              = local.use_ecs ? 1 : 0
  name               = "${local.name_prefix}-ecs-exec"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume[0].json
  tags               = local.common_tags
}

resource "aws_iam_role_policy_attachment" "ecs_execution" {
  count      = local.use_ecs ? 1 : 0
  role       = aws_iam_role.ecs_execution[0].name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# Permite a execution role ler os segredos (RDS/MQ) na task definition.
resource "aws_iam_role_policy" "ecs_execution_secrets" {
  count = local.use_ecs ? 1 : 0
  name  = "${local.name_prefix}-ecs-exec-secrets"
  role  = aws_iam_role.ecs_execution[0].id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = ["secretsmanager:GetSecretValue"]
      Resource = [
        aws_secretsmanager_secret.db.arn,
        aws_secretsmanager_secret.mq.arn,
        aws_secretsmanager_secret.crypto.arn,
        aws_secretsmanager_secret.db_app.arn
      ]
    }]
  })
}

# --- IAM: task role (a aplicacao acessa os buckets S3) ---
resource "aws_iam_role" "ecs_task" {
  count              = local.use_ecs ? 1 : 0
  name               = "${local.name_prefix}-ecs-task"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume[0].json
  tags               = local.common_tags
}

resource "aws_iam_role_policy" "ecs_task_s3" {
  count = local.use_ecs ? 1 : 0
  name  = "${local.name_prefix}-ecs-task-s3"
  role  = aws_iam_role.ecs_task[0].id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = [
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject",
        "s3:ListBucket"
      ]
      Resource = concat(
        [for b in aws_s3_bucket.this : b.arn],
        [for b in aws_s3_bucket.this : "${b.arn}/*"]
      )
    }]
  })
}

resource "aws_ecs_cluster" "this" {
  count = local.use_ecs ? 1 : 0
  name  = "${local.name_prefix}-cluster"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }

  tags = local.common_tags
}

resource "aws_ecs_cluster_capacity_providers" "this" {
  count        = local.use_ecs ? 1 : 0
  cluster_name = aws_ecs_cluster.this[0].name
  capacity_providers = ["FARGATE", "FARGATE_SPOT"]

  default_capacity_provider_strategy {
    capacity_provider = "FARGATE"
    weight            = 1
  }
}

resource "aws_cloudwatch_log_group" "backend" {
  count             = local.use_ecs ? 1 : 0
  name              = "/ecs/${local.name_prefix}-backend"
  retention_in_days = var.environment == "prod" ? 90 : 14
  tags              = local.common_tags
}

# --- Application Load Balancer publico ---
resource "aws_lb" "this" {
  count              = local.use_ecs ? 1 : 0
  name               = "${local.name_prefix}-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb[0].id]
  subnets            = module.vpc.public_subnets

  tags = local.common_tags
}

resource "aws_lb_target_group" "backend" {
  count       = local.use_ecs ? 1 : 0
  name        = "${local.name_prefix}-backend-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = module.vpc.vpc_id
  target_type = "ip"

  health_check {
    path                = "/actuator/health" # Spring Boot Actuator
    matcher             = "200"
    interval            = 30
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  tags = local.common_tags
}

locals {
  has_tls = var.acm_certificate_arn != ""
}

# Target group do painel web (SPA React servido pelo nginx).
resource "aws_lb_target_group" "web" {
  count       = local.use_ecs ? 1 : 0
  name        = "${local.name_prefix}-web-tg"
  port        = 80
  protocol    = "HTTP"
  vpc_id      = module.vpc.vpc_id
  target_type = "ip"

  health_check {
    path                = "/"
    matcher             = "200"
    interval            = 30
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  tags = local.common_tags
}

# Listener HTTP (80): serve o painel web por padrao; /api/* e /actuator/* vao para o backend.
# Recomendacao em prod: com acm_certificate_arn definido, use o 443 e redirecione 80 -> 443.
resource "aws_lb_listener" "http" {
  count             = local.use_ecs ? 1 : 0
  load_balancer_arn = aws_lb.this[0].arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.web[0].arn
  }
}

resource "aws_lb_listener_rule" "http_backend" {
  count        = local.use_ecs ? 1 : 0
  listener_arn = aws_lb_listener.http[0].arn
  priority     = 10

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.backend[0].arn
  }

  condition {
    path_pattern {
      values = ["/api/*", "/actuator/*"]
    }
  }
}

# Listener HTTPS (443): so quando ha certificado ACM. Mesma regra de roteamento.
resource "aws_lb_listener" "https" {
  count             = local.use_ecs && local.has_tls ? 1 : 0
  load_balancer_arn = aws_lb.this[0].arn
  port              = 443
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-2021-06"
  certificate_arn   = var.acm_certificate_arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.web[0].arn
  }
}

resource "aws_lb_listener_rule" "https_backend" {
  count        = local.use_ecs && local.has_tls ? 1 : 0
  listener_arn = aws_lb_listener.https[0].arn
  priority     = 10

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.backend[0].arn
  }

  condition {
    path_pattern {
      values = ["/api/*", "/actuator/*"]
    }
  }
}

# --- Task definition do backend ---
resource "aws_ecs_task_definition" "backend" {
  count                    = local.use_ecs ? 1 : 0
  family                   = "${local.name_prefix}-backend"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.fargate_cpu
  memory                   = var.fargate_memory
  execution_role_arn       = aws_iam_role.ecs_execution[0].arn
  task_role_arn            = aws_iam_role.ecs_task[0].arn

  container_definitions = jsonencode([
    {
      name      = "backend"
      image     = var.backend_container_image != "" ? var.backend_container_image : "${aws_ecr_repository.this["backend"].repository_url}:latest"
      essential = true
      portMappings = [
        { containerPort = 8080, protocol = "tcp" }
      ]
      # Nomes ALINHADOS ao que o backend realmente lê (application.yml / application-prod.yml).
      environment = [
        { name = "SPRING_PROFILES_ACTIVE", value = "prod" },
        { name = "AWS_REGION", value = var.aws_region },
        # JDBC completo (o app lê DB_URL, não DB_HOST).
        { name = "DB_URL", value = "jdbc:postgresql://${aws_db_instance.postgres.address}:5432/${var.db_name}" },
        # Runtime roda como o role de aplicação (RLS); Flyway roda como o dono (migration).
        { name = "DB_APP_USER", value = var.db_app_username },
        { name = "DB_MIGRATION_USER", value = var.db_username },
        # Validação dos tokens dos admins (Keycloak). Sem isto o resource server não valida nada.
        { name = "KEYCLOAK_JWKS_URI", value = var.keycloak_jwks_uri },
        # Nome que o Spring Boot realmente lê (relaxed binding → spring.data.redis.host).
        # Com isto o rate-limit anti-abuso e a revogação de token passam a ser COMPARTILHADOS
        # entre as réplicas — pré-requisito para backend_desired_count > 1.
        { name = "SPRING_DATA_REDIS_HOST", value = aws_elasticache_replication_group.redis.primary_endpoint_address },
        { name = "SPRING_DATA_REDIS_PORT", value = "6379" },
        { name = "SPRING_DATA_REDIS_SSL_ENABLED", value = "true" },
        { name = "S3_BUCKET_FOTOS", value = aws_s3_bucket.this["fotos"].bucket },
        { name = "S3_BUCKET_ATESTADOS", value = aws_s3_bucket.this["atestados"].bucket },
        { name = "S3_BUCKET_AFD", value = aws_s3_bucket.this["afd"].bucket }
      ]
      # Segredos injetados a partir do Secrets Manager (chaves do JSON, exceto o crypto que é string).
      secrets = [
        { name = "DB_MIGRATION_PASSWORD", valueFrom = "${aws_secretsmanager_secret.db.arn}:password::" },
        { name = "DB_APP_PASSWORD", valueFrom = "${aws_secretsmanager_secret.db_app.arn}:password::" },
        { name = "PONTO_CRYPTO_SECRET", valueFrom = aws_secretsmanager_secret.crypto.arn },
        { name = "MQ_USERNAME", valueFrom = "${aws_secretsmanager_secret.mq.arn}:username::" },
        { name = "MQ_PASSWORD", valueFrom = "${aws_secretsmanager_secret.mq.arn}:password::" }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.backend[0].name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "backend"
        }
      }
    }
  ])

  tags = local.common_tags
}

resource "aws_ecs_service" "backend" {
  count           = local.use_ecs ? 1 : 0
  name            = "${local.name_prefix}-backend"
  cluster         = aws_ecs_cluster.this[0].id
  task_definition = aws_ecs_task_definition.backend[0].arn
  desired_count   = var.backend_desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = module.vpc.private_subnets
    security_groups  = [aws_security_group.app.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.backend[0].arn
    container_name   = "backend"
    container_port   = 8080
  }

  depends_on = [aws_lb_listener.http]

  tags = local.common_tags
}

# --- Painel web (SPA React/nginx) como serviço ECS atrás do mesmo ALB ---
resource "aws_cloudwatch_log_group" "web" {
  count             = local.use_ecs ? 1 : 0
  name              = "/ecs/${local.name_prefix}-web"
  retention_in_days = var.environment == "prod" ? 90 : 14
  tags              = local.common_tags
}

resource "aws_ecs_task_definition" "web" {
  count                    = local.use_ecs ? 1 : 0
  family                   = "${local.name_prefix}-web"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 256
  memory                   = 512
  execution_role_arn       = aws_iam_role.ecs_execution[0].arn

  container_definitions = jsonencode([
    {
      name         = "web"
      image        = var.web_container_image != "" ? var.web_container_image : "${aws_ecr_repository.this["web"].repository_url}:latest"
      essential    = true
      portMappings = [{ containerPort = 80, protocol = "tcp" }]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.web[0].name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "web"
        }
      }
    }
  ])

  tags = local.common_tags
}

resource "aws_ecs_service" "web" {
  count           = local.use_ecs ? 1 : 0
  name            = "${local.name_prefix}-web"
  cluster         = aws_ecs_cluster.this[0].id
  task_definition = aws_ecs_task_definition.web[0].arn
  desired_count   = var.web_desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = module.vpc.private_subnets
    security_groups  = [aws_security_group.app.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.web[0].arn
    container_name   = "web"
    container_port   = 80
  }

  depends_on = [aws_lb_listener.http]

  tags = local.common_tags
}

# ----------------------------------------------------------------------------
# 9b) EKS (opcional). Habilite com container_platform = "eks".
#     Usa o modulo oficial terraform-aws-modules/eks/aws.
# ----------------------------------------------------------------------------
module "eks" {
  count   = local.use_eks ? 1 : 0
  source  = "terraform-aws-modules/eks/aws"
  version = "~> 20.0"

  cluster_name    = "${local.name_prefix}-eks"
  cluster_version = var.eks_cluster_version

  vpc_id     = module.vpc.vpc_id
  subnet_ids = module.vpc.private_subnets

  cluster_endpoint_public_access = true

  eks_managed_node_groups = {
    default = {
      instance_types = var.eks_node_instance_types
      min_size       = 1
      max_size       = var.eks_node_desired_size + 2
      desired_size   = var.eks_node_desired_size
    }
  }

  tags = local.common_tags
}
