# Passo a passo de implantação — o que falta para colocar uma prefeitura em produção

> Estado atual (2026-08-05): backend **240 testes verdes**, `web` build + vitest OK, `flutter analyze`
> limpo. **Todos os bloqueadores de CÓDIGO do go-live foram fechados** (auditoria multi-agente de
> 2026-08-04 + lote 37). O que resta é **infraestrutura, credenciais e decisões legais** — cada item
> abaixo diz o que já está pronto no código e o passo externo que ativa a peça.
>
> Regra geral do projeto: tudo que depende de serviço externo é um **seam** — um default seguro
> (no-op) + uma implementação real que liga sozinha quando a configuração aparece. Ativar = definir
> variáveis; **nenhum item exige mais desenvolvimento**.

Legenda: ✅ pronto e ativo · 🟡 código pronto, ativação por config/credencial · 🔴 processo externo (jurídico/administrativo).

---

## 0. Segurança de isolamento — o que mudou (IMPORTANTE para o deploy)

Correções aplicadas no lote 37 que **mudam o comportamento em produção**:

1. **Cabeçalho `X-Tenant-Id` não vale mais para requisições autenticadas.** O tenant do admin vem
   SÓ do claim `tenant_id` do JWT. Token sem o claim → contexto vazio → RLS nega (fail-closed).
   Consequência: **o mapper `tenant_id` no Keycloak é obrigatório** (já versionado no realm, ver 2.1)
   e **cada usuário admin precisa do atributo `tenant_id`** = UUID do ente.
2. **RLS agora exige role de banco não-superusuário.** O perfil `prod` conecta o runtime como
   `DB_APP_USER` (NOSUPERUSER/NOBYPASSRLS) e roda o Flyway como `DB_MIGRATION_USER` (dono). Sem
   isso, `FORCE RLS` não vale (superusuário ignora RLS). Ver 1.2.
3. **Perfil `prod` é fail-fast:** sem `PONTO_CRYPTO_SECRET`, `DB_APP_PASSWORD`, `KEYCLOAK_JWKS_URI`
   etc., a aplicação NÃO sobe (nada de defaults fracos de dev). Swagger desligado; `ponto.security.open`
   forçado para `false`.
4. **Backend não publica mais a porta 8080** no compose de prod; só o nginx o alcança. O nginx
   **sobrescreve** `X-Forwarded-For` (anti-spoof do rate-limit) e envia headers de segurança.
5. **AFD**: agora inclui registros **tipo 5 (empregados)** e **recusa emitir sem o CNPJ do ente**
   (erro 400 orientando a cadastrar). O CNPJ é cadastrado no painel (Identidade visual → CNPJ) ou
   via `PUT /api/branding/cnpj`.

---

## 1. Subir o ambiente de produção (infra pura)

### 1.1 Escolha o caminho: compose single-host OU AWS/Terraform

**A) Compose (single-host, piloto pequeno):** `infra/docker-compose.prod.yml`
```bash
cd infra
DB_PASSWORD=... DB_APP_PASSWORD=... PONTO_CRYPTO_SECRET=... KEYCLOAK_JWKS_URI=... docker compose -f docker-compose.prod.yml up -d
```
- Variáveis obrigatórias (fail-fast): `DB_PASSWORD`, `DB_APP_PASSWORD`, `PONTO_CRYPTO_SECRET`, `KEYCLOAK_JWKS_URI`.
- O init do Postgres cria automaticamente o role de aplicação (`infra/postgres/10-create-app-role.sh`).
- O backend sobe com `SPRING_PROFILES_ACTIVE=prod` (já configurado no compose).
- **TLS**: coloque um proxy com HTTPS na frente do nginx (Caddy/Traefik/Certbot ou LB da nuvem).
  Depois de 100% em HTTPS, habilite o header HSTS comentado em `web/nginx.conf`.

**B) AWS (Terraform, `infra/terraform/`):** VPC, RDS Multi-AZ, ALB, ECS Fargate (backend + web),
ECR, S3, Secrets Manager — tudo pronto, incluindo (lote 37):
- Task ECS com os nomes de env **corretos** (`DB_URL`, `DB_APP_USER`, `DB_MIGRATION_USER`,
  `KEYCLOAK_JWKS_URI`) e segredos (`PONTO_CRYPTO_SECRET`, senhas) via Secrets Manager.
- **Serviço web (painel)** atrás do mesmo ALB: `/api/*` e `/actuator/*` → backend; resto → web.
- **Listener HTTPS 443**: defina `acm_certificate_arn` no tfvars (certificado ACM do domínio).
```bash
cd infra/terraform
terraform init
terraform plan  -var-file=environments/prod.tfvars
terraform apply -var-file=environments/prod.tfvars
```
- Passos manuais pós-apply:
  1. Criar o **role de aplicação** no RDS: rodar o SQL de `infra/postgres/10-create-app-role.sh`
     (via bastion/psql) usando a senha do segredo `<prefixo>/rds/app` do Secrets Manager.
  2. Definir `keycloak_jwks_uri` e `acm_certificate_arn` no tfvars.
  3. Build/push das imagens para o ECR (backend e web) e atualizar os services.
- **Multi-réplica (`backend_desired_count = 3`) é seguro**: o rate-limit anti-abuso, o cache/revogação
  de token de dispositivo e a trava do agendador usam **Redis** (a task já recebe
  `SPRING_DATA_REDIS_HOST`). Sem Redis, tudo cai para memória — correto apenas com **1 réplica**.

### 1.2 Banco de dados — role de aplicação (RLS)
- **Por quê:** `FORCE RLS` não se aplica a superusuário. O runtime PRECISA conectar como role comum.
- **Pronto:** `application-prod.yml` separa runtime (`DB_APP_USER`) de migrations (`DB_MIGRATION_USER`);
  script `infra/postgres/10-create-app-role.sh` cria o role com os grants + default privileges.
- **Passo:** no compose é automático (primeira inicialização). No RDS, rodar o SQL do script uma vez.

### 1.3 Domínio + DNS + TLS 🔴
- Registrar/apontar o domínio (ideal: `.gov.br` do ente ou domínio do SaaS com subdomínio por ente).
- Emitir certificado (ACM na AWS; Certbot no single-host). **Não operar em HTTP.**

---

## 2. Keycloak (identidade dos administradores) 🟡

### 2.1 Subir o Keycloak de produção
- **Pronto no repo:** realm versionado em `infra/keycloak/ponto-realm.json` com: roles
  (servidor/gestor/rh/controladoria/tenant-admin/operador), **MFA/OTP condicional para perfis
  administrativos**, client SPA `ponto-web` (PKCE S256) e **o protocol mapper `tenant_id`**
  (obrigatório — injeta o atributo do usuário como claim no token).
- **Passos:**
  1. Provisionar um Keycloak com **banco persistente** (não o H2 de dev): container próprio,
     Keycloak gerenciado, ou serviço equivalente.
  2. Importar o realm `ponto` a partir do JSON versionado.
  3. Ajustar para o domínio real: `redirectUris`/`webOrigins` do client `ponto-web`
     (ex.: `https://painel.SEUDOMINIO/*`) — os valores versionados são localhost (dev).
  4. Trocar a senha do admin do console; configurar SMTP do realm (e-mails de reset).
  5. Backend: `KEYCLOAK_JWKS_URI=https://KEYCLOAK/realms/ponto/protocol/openid-connect/certs`.
  6. Web (build): `VITE_OIDC_AUTHORITY=https://KEYCLOAK/realms/ponto` e
     `VITE_OIDC_CLIENT_ID=ponto-web`. Sem essas envs o painel cai no login dev e **exibe um alerta
     vermelho** em build de produção.

### 2.2 Provisionamento do 1º admin de cada ente 🟡 (automatizável)
- **Pronto:** ao **aprovar a adesão** de um ente, o backend chama o seam `ProvisionadorIdentidade`:
  - **Sem config** → registra no log um runbook (criar usuário manualmente com o atributo
    `tenant_id` = UUID do ente + role `tenant-admin`).
  - **Com config** → `KeycloakAdminProvisionador` cria o usuário sozinho via API de admin
    (atributo `tenant_id`, role `tenant-admin`, e-mail para definir senha).
- **Passos para automatizar:**
  1. No Keycloak, criar um client confidencial `ponto-provisioner` com service account e a role
     `manage-users` (realm-management).
  2. Backend: `keycloak.admin.server-url=https://KEYCLOAK`, `keycloak.admin.client-id=ponto-provisioner`,
     `keycloak.admin.client-secret=...` (via secret). Opcional: `keycloak.admin.realm` (default `ponto`).
- **Manual (fallback):** console do Keycloak → Users → Create: e-mail do responsável, atributo
  `tenant_id` = UUID do ente (aparece na aprovação da adesão e em `/api/tenants`), role `tenant-admin`.

---

## 3. Assinatura ICP-Brasil do AFD 🟡 + 🔴

### 3.1 O certificado (decisão + aquisição) 🔴
**Você (desenvolvedor) NÃO precisa de certificado pessoal.** A assinatura usa **e-CNPJ** (pessoa jurídica):

| Modelo | Quem assina | Quem obtém | Recomendado p/ SaaS público? |
|--------|-------------|------------|------------------------------|
| **A — Ente assina** | e-CNPJ de cada município/órgão | representante legal do ente | ✅ Sim — o ente é o empregador |
| **B — Fornecedor assina** | e-CNPJ do fornecedor | sua empresa (1 cert p/ todos) | Possível; decisão jurídica/contratual |

Passos para o município obter o e-CNPJ: AC credenciada ITI (Serpro, Serasa, Certisign, Valid,
Soluti, Safeweb) → **e-CNPJ A1** (arquivo `.p12`/`.pfx`, recomendado p/ servidor) → validação do
representante legal → entrega segura do arquivo + senha. Detalhes: `docs/afd-assinatura-icp-brasil.md`.

### 3.2 Ativar a assinatura no backend 🟡 (código PRONTO — lote 37)
- **Pronto:** `AssinaturaCadesService` — assinatura **CAdES/PKCS#7 destacada** (BouncyCastle) do AFD,
  com teste que verifica criptograficamente o CMS. Liga sozinha quando a keystore é configurada;
  sem config, o AFD sai com hash SHA-256 e `assinatura=null` (comportamento atual).
- **Passos:**
  1. Guardar o `.p12` num caminho seguro do servidor (ou secret montado em arquivo).
  2. Configurar (env ou yaml):
     ```
     ASSINATURA_KEYSTORE=/caminho/ecnpj.p12   → assinatura.keystore
     ASSINATURA_SENHA=...                     → assinatura.senha
     ASSINATURA_ALIAS=...                     → assinatura.alias (opcional)
     ```
  3. Reiniciar. Se a keystore estiver errada, a aplicação **não sobe** (fail-fast de propósito —
     melhor do que emitir AFD sem assinar achando que assinou).
  4. O `AfdResponse.assinatura` passa a trazer o `.p7s` (base64, destacado).
- **Nota multi-tenant:** a config atual é **uma keystore por instância** (Modelo B, ou Modelo A com
  uma instância/config por ente). Keystore por tenant dinâmica = evolução futura se necessário.

---

## 4. Notificações (e-mail e push) 🟡

- **Pronto (lote 37):** `RoteadorNotificador` roteia por canal para adaptadores reais, com fallback
  em log (nunca quebra). As notificações **já são persistidas e lidas in-app** — e-mail/push são
  reforço, não requisito do piloto.
- **E-mail (SMTP/SES) — só config:**
  ```
  spring.mail.host=email-smtp.sa-east-1.amazonaws.com   (ou SMTP do ente)
  spring.mail.port=587
  spring.mail.username=... / spring.mail.password=...
  spring.mail.properties.mail.smtp.auth=true
  spring.mail.properties.mail.smtp.starttls.enable=true
  notificacao.email.remetente=nao-responder@SEUDOMINIO
  ```
- **Push (FCM) — só config:** o `FcmPushSender` já implementa a **API HTTP v1** (JWT da conta de
  serviço → access token → envio). Ative com:
  ```
  fcm.credenciais=/seguro/service-account.json   (JSON da conta de serviço do projeto Firebase)
  ```
  O `destinatario` da notificação deve ser o *registration token* do aparelho. **Falta no app:**
  adicionar `firebase_messaging` + `google-services.json` e registrar o token do aparelho no
  backend — é o único pedaço que depende do projeto Firebase do cliente.

### 4.1 Redis (obrigatório com mais de uma réplica) 🟡
- **Pronto:** rate-limit anti-abuso (`ContadorJanelaRedis`), cache + revogação global de token de
  dispositivo (`CacheDispositivoRedis`) e trava do agendador (`TravaRedis`). Todos com degradação
  graciosa: Redis fora do ar → consulta o banco (autenticação nunca falha por causa de cache).
- **Ative com:** `SPRING_DATA_REDIS_HOST` (+ `_PORT`, `_PASSWORD`, `_SSL_ENABLED`). Sem isso, tudo
  roda em memória — correto **apenas com 1 réplica**.
- O health check do Redis fica desligado de propósito: uma oscilação não pode tirar a instância do
  balanceador, já que o sistema continua correto sem ele.

---

## 5. Onboarding de uma prefeitura real (roteiro operacional)

1. **Adesão:** o ente pede em `/aderir` (público) OU o operador cria direto (`POST /api/tenants`).
2. **Aprovação:** operador aprova em "Adesão de entes" → tenant provisionado + (se 2.2 configurado)
   admin criado no Keycloak automaticamente.
3. **Login do admin do ente:** via Keycloak (define a senha pelo e-mail recebido).
4. **Configuração inicial do ente (painel):**
   - Identidade visual → **CNPJ do ente** (obrigatório p/ AFD), nome do app, logo, cores, subdomínio.
   - Órgãos/lotações (+ regras de ponto por órgão: tolerância, banco de horas, geofence, teletrabalho).
   - Jornadas + horários; escalas (inclusive 12×36); calendário oficial (feriados/facultativos).
   - Contrato (modalidade, empenho, vigência, valores) — página do operador.
5. **Servidores:** importação CSV (com `lotacaoSigla` — cada servidor já entra lotado) ou cadastro
   manual; migração de saldo de banco de horas (`/api/migracao/banco-horas`, CSV `matricula;saldoMinutos`).
6. **Ativação dos aparelhos:** RH gera código por vínculo → servidor ativa no app (ou na página
   `/servidor` pela web). Sem gov.br: código + token de dispositivo.
7. **Rotina:** ponto → apuração → correções/justificativas → fechamento → ciência → espelho/PDF →
   AFD assinado → relatórios TCM/IN-008 → export p/ folha (CSV).
8. **Agendador (lembretes):** ligar com `ponto.agendador.enabled=true` em UMA instância (cron
   configurável; default dias úteis 8h). Expurgo LGPD entra aí quando a política de retenção for
   definida (decisão jurídica).

---

## 6. App mobile (lojas) 🔴 + 🟡

- **Pronto:** release Android assinado (keystore fora do git), Shorebird OTA preparado
  (`mobile/shorebird.yaml`, falta `shorebird init` da conta), TTS/STT, biometria local.
- **Build de release (obrigatório apontar o backend):**
  ```bash
  flutter build appbundle --dart-define=API_BASE_URL=https://api.SEUDOMINIO
  ```
  **Sem o `--dart-define`, o release fica com URL vazia e falha visivelmente** (guard-rail do lote
  37 — nunca cai no localhost/cleartext de dev).
- **Passos externos:** conta Google Play (+ Apple se iOS) → ficha da loja → **política de
  privacidade pública** + formulário Data Safety/App Privacy (o app coleta biometria facial,
  localização e áudio — declarar) → revisão. Recomendado: trocar a senha placeholder da keystore por
  uma forte e aderir ao Play App Signing.

---

## 7. Jurídico/compliance (não é código) 🔴

| Item | Status | Passo |
|------|--------|-------|
| **DPA/contrato de operador** (LGPD art. 39) | pendente | assinar com cada ente (controlador=ente, operador=SaaS) |
| **Encarregado (DPO)** | pendente | nomear e publicar o contato |
| **DPIA/RIPD** | minuta no repo | finalizar/assinar (pendências: política de retenção, pentest) |
| **Decisão do modelo de assinatura** (seção 3.1) | pendente | jurídico define A ou B |
| **Auditoria WCAG/eMAG certificada** | correções feitas | contratar avaliação formal (risco em edital) |
| **Restore/DR validado** | scripts prontos | executar um drill de restore em staging |
| **Pentest** | — | contratar antes do go-live (recomendado) |

---

## 8. Checklist final de go-live (resumo executável)

- [ ] Infra no ar (compose+TLS ou Terraform+ACM) com segredos fortes (Secrets Manager)
- [ ] Role de aplicação criado no Postgres (RLS ativa — seção 1.2)
- [ ] Keycloak prod: realm importado, redirects do domínio real, mapper `tenant_id` (vem no JSON)
- [ ] Web buildado com `VITE_OIDC_*` (sem alerta vermelho na tela de login)
- [ ] `keycloak.admin.*` configurado (1º admin automático) OU runbook manual acordado
- [ ] Ente piloto criado, CNPJ cadastrado, AFD emitindo (assinado, se e-CNPJ disponível)
- [ ] SMTP configurado (`spring.mail.host`) — e-mails saindo
- [ ] `SPRING_DATA_REDIS_HOST` configurado se houver mais de uma réplica
- [ ] App release com `API_BASE_URL` HTTPS publicado (ou distribuição interna no piloto)
- [ ] `ponto.agendador.enabled=true` (pode ficar em todas as réplicas — a trava evita duplicidade)
- [ ] DPA assinado + DPO nomeado + política de privacidade publicada
- [ ] Drill de restore executado; monitoramento/alertas básicos ligados

> **Estado honesto:** não há desenvolvimento pendente para o go-live — os itens acima são
> provisionamento, credenciais e assinaturas. O que **permanece em aberto e por quê**:
>
> | Item | Situação |
> |------|----------|
> | **Registro tipo 6 do AFD + leiaute oficial do AEJ** | Exige o **Anexo vigente da Portaria 671** (posições e tamanhos exatos de cada campo). Não dá para inferir de memória sem risco de gerar arquivo rejeitado — precisa do documento oficial em mãos para implementar e homologar no programa de tratamento do MTP. |
> | **Registro do token FCM pelo app** | O envio (backend) está pronto; falta o app obter o *registration token* do Firebase e registrá-lo — depende do projeto Firebase do cliente. |
> | **Keystore de assinatura por tenant** | Hoje é uma keystore por instância (modelo "fornecedor assina" ou uma instância por ente). Multi-keystore dinâmica só faz sentido depois da decisão jurídica da seção 3.1. |
> | **Auditoria WCAG certificada, pentest, DPA/DPO** | Processos externos (seção 7). |
