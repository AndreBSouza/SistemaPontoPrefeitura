# Infraestrutura como Codigo (Terraform) - Ponto Municipal

IaC para provisionar a infraestrutura do **Ponto Municipal** (SaaS de ponto
eletronico para servidores publicos municipais) na **AWS**, regiao
**`sa-east-1` (Sao Paulo, Brasil)**.

> Esqueleto funcional e comentado. Revise sizing, janelas de manutencao,
> certificados (ACM/HTTPS) e politicas de IAM antes de aplicar em producao.

---

## O que e provisionado

| Recurso | Servico AWS | Observacoes |
|---|---|---|
| Rede | VPC + subnets publicas e privadas (Multi-AZ) | NAT Gateway, IGW, route tables (modulo oficial) |
| Containers | **ECS Fargate** (padrao) ou **EKS** | Selecionavel via `container_platform` |
| Banco de dados | RDS **PostgreSQL Multi-AZ** | Criptografado, backups automaticos, Performance Insights |
| Cache | ElastiCache **Redis** | Replication group com failover, cripto em repouso e transito |
| Mensageria | **Amazon MQ (RabbitMQ)** | Broker privado, AMQPS |
| Armazenamento | **S3**: `fotos`, `atestados`, `afd` | Versionamento + criptografia (KMS) + bloqueio publico + lifecycle |
| Registro de imagens | **ECR**: `backend`, `web` | Scan on push, lifecycle (mantem 20 imagens) |
| Segredos | **Secrets Manager** | Senhas de RDS e MQ geradas automaticamente |
| Observabilidade | CloudWatch Logs / Container Insights | |

---

## Pre-requisitos

- [Terraform](https://developer.hashicorp.com/terraform/downloads) **>= 1.5**
- AWS CLI configurada
- **Credenciais sao do cliente** (ver secao abaixo)

---

## Credenciais (sao do cliente)

Este projeto **nao contem nem versiona credenciais**. Quem aplica o Terraform
deve fornecer credenciais da **conta AWS do cliente**, por um destes meios:

```bash
# Opcao 1: variaveis de ambiente
export AWS_ACCESS_KEY_ID="..."
export AWS_SECRET_ACCESS_KEY="..."
export AWS_DEFAULT_REGION="sa-east-1"

# Opcao 2: perfil nomeado (~/.aws/credentials)
export AWS_PROFILE="ponto-cliente"

# Opcao 3: AWS SSO / IAM Role (recomendado)
aws sso login --profile ponto-cliente
```

As senhas do RDS e do Amazon MQ sao **geradas automaticamente** e guardadas no
**AWS Secrets Manager** (veja os outputs `rds_secret_arn` e `mq_secret_arn`).
Nunca commite `terraform.tfstate`, `*.tfvars` com segredos, nem chaves.

---

## Backend remoto de state (S3 + DynamoDB)

O state e armazenado de forma remota em **S3** com **lock** via **DynamoDB**.
Esses dois recursos precisam existir **antes** do primeiro `init` (problema do
"ovo e a galinha"), entao crie-os uma unica vez por conta:

```bash
# Bucket de state (com versionamento e criptografia)
aws s3api create-bucket \
  --bucket ponto-municipal-tfstate \
  --region sa-east-1 \
  --create-bucket-configuration LocationConstraint=sa-east-1

aws s3api put-bucket-versioning \
  --bucket ponto-municipal-tfstate \
  --versioning-configuration Status=Enabled

aws s3api put-bucket-encryption \
  --bucket ponto-municipal-tfstate \
  --server-side-encryption-configuration \
  '{"Rules":[{"ApplyServerSideEncryptionByDefault":{"SSEAlgorithm":"aws:kms"}}]}'

# Tabela de lock
aws dynamodb create-table \
  --table-name ponto-municipal-tflock \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region sa-east-1
```

O bloco `backend "s3"` em `versions.tf` esta com os valores **comentados** de
proposito; passe-os no `init` (assim o mesmo codigo serve a qualquer cliente):

```bash
terraform init \
  -backend-config="bucket=ponto-municipal-tfstate" \
  -backend-config="key=ponto/terraform.tfstate" \
  -backend-config="region=sa-east-1" \
  -backend-config="dynamodb_table=ponto-municipal-tflock" \
  -backend-config="encrypt=true"
```

> Dica: use uma `key` diferente por ambiente, ex.:
> `ponto/dev/terraform.tfstate`, `ponto/staging/...`, `ponto/prod/...`.

---

## Uso (init / plan / apply)

```bash
# 1. Inicializar (baixa providers/modulos e configura o backend)
terraform init -backend-config=...   # ver secao acima

# 2. Validar e formatar
terraform fmt -recursive
terraform validate

# 3. Planejar (exemplo: ambiente dev)
terraform plan -var-file="environments/dev.tfvars"

# 4. Aplicar
terraform apply -var-file="environments/dev.tfvars"

# 5. Destruir (cuidado!)
terraform destroy -var-file="environments/dev.tfvars"
```

---

## Ambientes: dev / staging / prod

Ha duas abordagens suportadas. Escolha **uma** e seja consistente.

### Opcao A — tfvars por ambiente (recomendada)

Arquivos prontos em `environments/`:

```bash
terraform apply -var-file="environments/dev.tfvars"
terraform apply -var-file="environments/staging.tfvars"
terraform apply -var-file="environments/prod.tfvars"
```

Combine com uma `key` de state distinta por ambiente (ver acima).

### Opcao B — workspaces

```bash
terraform workspace new dev
terraform workspace new staging
terraform workspace new prod

terraform workspace select prod
terraform apply -var-file="environments/prod.tfvars"
```

Com workspaces, o S3 backend isola o state em
`env:/<workspace>/<key>` automaticamente.

---

## Selecionar ECS Fargate vs EKS

Padrao e **ECS Fargate** (mais simples e barato para este perfil de carga).
Para usar **EKS**, defina no tfvars:

```hcl
container_platform = "eks"
```

Recursos especificos de cada plataforma sao criados condicionalmente
(`count`), entao alternar nao deixa lixo da outra plataforma.

---

## Pos-deploy (checklist)

- [ ] Adicionar **listener HTTPS (443)** no ALB com certificado **ACM** e
      redirecionar 80 -> 443 (obrigatorio em prod).
- [ ] Publicar a imagem do backend no ECR (`ecr_repository_urls`) e atualizar o
      service ECS (ou apontar `backend_container_image`).
- [ ] Configurar **Keycloak/OIDC** (fora deste Terraform; pode ser RDS proprio
      ou servico gerenciado).
- [ ] Revisar regras de **RLS multi-tenant** no Postgres (camada de aplicacao).
- [ ] Habilitar `db_deletion_protection = true` e `db_multi_az = true` em prod.
- [ ] Configurar alarmes CloudWatch e backup/retencao conforme exigencia legal
      (atestados e AFD sao documentos com guarda obrigatoria).

---

## Estrutura de arquivos

```
infra/terraform/
├── versions.tf      # versoes + provider AWS + backend S3
├── variables.tf     # variaveis de entrada (regiao, sizing, tags...)
├── main.tf          # VPC, ECS/EKS, RDS, Redis, MQ, S3, ECR, IAM, segredos
├── outputs.tf       # saidas (endpoints, ARNs, URLs)
├── README.md        # este arquivo
└── environments/
    ├── dev.tfvars
    ├── staging.tfvars
    └── prod.tfvars
```
