# Gestão de Segredos — Ponto Municipal

Política e procedimentos para **gerir segredos** (senhas, chaves, tokens, certificados):
onde guardar, como rotacionar, uso de `.env` e a regra de ouro — **nunca commitar segredo**.

> Backends de segredo: **AWS Secrets Manager / SSM Parameter Store** (padrão na nuvem,
> sa-east-1) e **HashiCorp Vault** (quando há requisito de segredos dinâmicos/multi-ambiente).
> Cifragem com **AWS KMS**. Auth da aplicação via **OIDC/Keycloak**.

---

## 1. Regra de ouro

**Segredo nunca entra no repositório Git.** Nem em código, nem em `application.yml`,
nem em `docker-compose.yml`, nem em comentário, nem em histórico.

O `.gitignore` já ignora `.env` e `*.env` (mantendo `*.env.example`). Isso é uma rede de
segurança, **não** a defesa principal — a defesa é não escrever o segredo no arquivo versionado.

---

## 2. Classificação do que é segredo

| Tipo | Exemplos no Ponto Municipal |
|---|---|
| Credencial de banco | usuário/senha do PostgreSQL, URL com senha |
| Chaves de objeto | `MINIO_ROOT_USER`/`MINIO_ROOT_PASSWORD`, access/secret key S3 |
| Mensageria | usuário/senha do RabbitMQ |
| Identidade | client secret do Keycloak (OIDC), chaves de assinatura |
| Cache | senha do Redis (quando habilitada) |
| Criptografia | chaves KMS, chave de cifragem do template biométrico |
| Integrações | tokens de e-mail/SMS, chaves de antivírus, webhooks |

> Os valores triviais do `infra/docker-compose.yml` (`ponto/ponto`, `minio/minio12345`,
> `guest/guest`, `admin/admin`) são **apenas para desenvolvimento local** e **proibidos em
> qualquer ambiente compartilhado/produção**.

---

## 3. Onde fica cada segredo

| Ambiente | Backend de segredo | Como a app lê |
|---|---|---|
| **Dev local** | arquivo `.env` (não versionado) | docker-compose / `spring-boot:run` lê variáveis de ambiente |
| **Staging/Prod** | AWS Secrets Manager / SSM (cifrado por KMS) ou Vault | injetado como variável de ambiente / Spring Cloud config no boot |
| **CI/CD (GitLab)** | CI/CD Variables **mascaradas e protegidas** | expostas só nos jobs necessários |

Princípios:
- A aplicação consome segredos como **variáveis de ambiente** (12-factor). Sem caminho de
  arquivo de segredo dentro da imagem.
- **Imagens Docker não contêm segredos** — injetados em runtime (task definition / secret refs).
- Acesso por **IAM de menor privilégio** (uma policy por serviço/ambiente; nada de chave mestra).
- Segredo cifrado em repouso (KMS) e em trânsito (TLS).

---

## 4. `.env` — uso correto

- `.env` existe **só em desenvolvimento local** e na máquina do dev; **nunca** sobe ao Git.
- Versionar **`.env.example`** com todas as chaves e **valores fictícios/placeholder**:

```dotenv
# .env.example — copie para .env e preencha localmente. NÃO commitar o .env real.
DB_URL=jdbc:postgresql://localhost:5432/ponto
DB_USER=ponto
DB_PASSWORD=changeme

REDIS_URL=redis://localhost:6379

RABBITMQ_USER=guest
RABBITMQ_PASSWORD=changeme

KEYCLOAK_ISSUER_URI=http://localhost:8081/realms/ponto
KEYCLOAK_CLIENT_ID=ponto-backend
KEYCLOAK_CLIENT_SECRET=changeme

S3_ENDPOINT=http://localhost:9000
S3_ACCESS_KEY=minio
S3_SECRET_KEY=changeme

# Chave de cifragem do template biométrico (dev: gerar local; prod: KMS)
BIOMETRIA_ENC_KEY=changeme
```

- Em produção, essas mesmas chaves vêm do Secrets Manager/SSM/Vault, **não** de `.env`.

---

## 5. Rotação de segredos

| Segredo | Frequência | Estratégia |
|---|---|---|
| Senha do banco | 90 dias (ou imediata em incidente) | Secrets Manager com rotação automática (Lambda) + dual-credential |
| Client secret Keycloak | 180 dias | Rotação no Keycloak + atualização do segredo na nuvem |
| Chaves S3/MinIO | 90 dias | Criar nova key, migrar, revogar antiga |
| Chave de assinatura JWT | conforme política do realm | Rotação de chave com JWKS (período de validade sobreposto) |
| Chave KMS / biometria | rotação de chave KMS habilitada | KMS key rotation anual; reencriptar conforme necessário |
| Variáveis de CI | 180 dias | Renovar e remover variáveis órfãs |

Boas práticas de rotação:
- **Rotação sem downtime:** suportar duas versões válidas durante a janela (a app lê a nova,
  a antiga ainda funciona até a troca completar) — alinhado ao blue/green do `docs/runbook-go-live.md`.
- **Rotação imediata** ao suspeitar de vazamento, ao desligar pessoa com acesso, ou após pentest.
- Rotação **automatizada** sempre que o backend suportar (Secrets Manager / Vault leases).

---

## 6. Resposta a vazamento de segredo

1. **Revogar/rotacionar** o segredo imediatamente (não basta remover do código).
2. Identificar o escopo: o que esse segredo dava acesso (banco? S3? biometria?).
3. **Remover do histórico do Git** (`git filter-repo`/BFG) e forçar push, se foi commitado;
   avisar quem clonou.
4. Investigar uso indevido (logs de acesso, CloudTrail, auditoria).
5. Se houver risco a dados pessoais/biometria, acionar **DPO** e avaliar notificação à ANPD
   (LGPD art. 48) — ver `docs/dpia-ripd.md`.
6. Postmortem + ação preventiva (ex.: pre-commit hook que faltou).

---

## 7. Prevenção e governança

- [ ] **Secret scanning** no CI (gitleaks/trufflehog) — bloqueia merge com segredo.
- [ ] **Pre-commit hook** local de detecção de segredos.
- [ ] Variáveis de CI **mascaradas + protegidas**; nunca ecoar segredo em log de pipeline.
- [ ] Acesso a segredos **auditado** (quem leu o quê e quando).
- [ ] Princípio do menor privilégio por serviço/ambiente; sem segredo compartilhado entre entes.
- [ ] Revisão periódica de segredos órfãos/expirados.
- [ ] Onboarding/offboarding: provisionar e **revogar** acessos a tempo.

> Documento vivo — revisar a cada nova integração que introduza credenciais.
