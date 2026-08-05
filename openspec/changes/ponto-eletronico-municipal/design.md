## Context

Proposta de um **SaaS de ponto eletrônico (controle de frequência) para servidores públicos municipais**, vendido a múltiplos entes (prefeituras, câmaras, autarquias, fundos). O produto nasce ancorado na **IN 008/2021 do TCM-GO**, que exige controle de frequência como parte do Sistema de Controle Interno (Atos de Pessoal), e deve suportar o modo **REP-P da Portaria MTP 671/2021** para vínculos celetistas, sob **LGPD**.

**Estado atual:** greenfield (não há base instalada). O cliente já domina o ecossistema **Java/Spring Boot + GitLab CI** e mantém sistemas municipais (contabilidade/folha) — fator considerado na escolha de stack para reaproveitar competência e facilitar integração.

**Restrições:**
- Conectividade instável no interior → registro **offline-first** é requisito, não opcional.
- Dados **sensíveis** (biometria facial) e pessoais (geolocalização) → cifragem, base legal e governança LGPD obrigatórias.
- Heterogeneidade dos regimes (estatutário/celetista/comissionado) e das jornadas (12x36, plantão, magistério) por ente → parametrização forte por tenant.
- Hospedagem em **região Brasil** por aderência LGPD e expectativa do setor público.

**Stakeholders:** servidor, chefia imediata, RH/Departamento de Pessoal, Controladoria Interna, gestor do ente (tenant admin), operador SaaS (super admin), e — como destinatário de evidências — o TCM e a fiscalização trabalhista.

## Goals / Non-Goals

**Goals:**
- Registro de ponto confiável e antifraude (geofencing + biometria facial com liveness) em **mobile, web e totem**, com **offline-first**.
- Apuração automática de frequência (ocorrências, banco de horas, adicional noturno) e **fechamento mensal** com espelho assinado eletronicamente.
- **Multi-tenancy** com isolamento de dados, onboarding self-service assistido e billing por assinatura.
- **Conformidade** verificável: IN 008/2021, Portaria 671/2021 (AFD/AEJ, NSR, comprovante), LGPD.
- Integração com **folha de pagamento** municipal e **eSocial**.
- Trilha de **auditoria imutável** ponta a ponta.

**Non-Goals (fora de escopo nesta fase):**
- Folha de pagamento completa (cálculo de proventos/descontos) — o sistema **exporta** insumos, não substitui a folha.
- Gestão de RH ampla (recrutamento, avaliação de desempenho, treinamento).
- Hardware proprietário de REP-C (relógio físico homologado) — suportamos REP-P (software) e integração com coletores existentes via AFD.
- BI corporativo avançado além dos dashboards operacionais previstos.

## Arquitetura (visão geral)

```
                         ┌─────────────────────────────────────────────┐
   Servidor / Gestor     │                  CLIENTES                    │
   ┌───────────┐         │  Mobile (Flutter)   Web (React/TS)   Totem   │
   │  App iOS  │◄────────┤   - registro          - RH/gestor     (PWA)  │
   │  App And. │         │   - offline+sync      - controladoria        │
   └───────────┘         └───────────────┬─────────────────────────────┘
                                          │ HTTPS / REST + JSON (OpenAPI)
                                          │ OIDC (tokens Keycloak)
                                  ┌───────▼─────────┐
                                  │   API Gateway    │  (rate-limit, WAF, TLS)
                                  └───────┬─────────┘
                                          │
        ┌─────────────────────────────────┼───────────────────────────────────┐
        │            BACKEND  —  Java 21 + Spring Boot 3 (monólito modular)      │
        │  iam/tenant │ cadastro │ jornada │ registro-ponto │ apuração │ banco-h │
        │  abono/aprov│ espelho  │ relatórios│ auditoria/AFD │ integração│ billing│
        └───┬─────────────┬───────────────┬──────────────────┬───────────┬──────┘
            │             │               │                  │           │
     ┌──────▼────┐  ┌─────▼─────┐   ┌─────▼──────┐    ┌──────▼─────┐ ┌───▼─────┐
     │PostgreSQL │  │  Redis    │   │ RabbitMQ   │    │ Object     │ │Keycloak │
     │(RLS/tenant)│ │(cache/    │   │(sync,      │    │ Storage S3 │ │ (IAM)   │
     │            │ │ sessão)   │   │ notif,     │    │(fotos,     │ │         │
     └────────────┘ └───────────┘   │ integr.)   │    │ atestados, │ └─────────┘
                                     └────────────┘    │ AFD/AEJ)   │
                                                        └────────────┘
   Externos:  Folha de pagamento  •  eSocial  •  gov.br  •  FCM (push)  •  e-mail (SES)
```

Padrão: **monólito modular** (módulos com fronteiras explícitas no mesmo deploy) — simples de operar no início e extraível para microsserviços (ex.: `registro-ponto`, `integração`) quando o volume exigir. Comunicação assíncrona via RabbitMQ para sincronização de marcações offline, notificações e integrações.

## Decisions

### D1 — Mobile: Flutter (alternativa: React Native)
**Escolha:** Flutter. **Por quê:** base de código única iOS/Android com desempenho próximo de nativo; excelente suporte a câmera, **ML on-device** (Google ML Kit para detecção facial/liveness), armazenamento local (SQLite/Drift) e *background sync* — críticos para offline e biometria. **Alternativa (RN):** reaproveita skill JS/TS do time web; aceitável, mas integrações nativas de câmera/biometria/■offline tendem a exigir mais módulos nativos. **Trade-off:** Dart é nova linguagem para o time → previsto onboarding.

### D2 — Backend: Java 21 + Spring Boot 3.x
**Escolha:** mantém a competência existente do time (Spring/Maven/GitLab), ecossistema maduro para segurança, transações e integrações. Estrutura **modular** (Maven multi-módulo ou Spring Modulith). **Alternativa:** .NET ou Node/NestJS — descartadas por não alavancarem a base instalada do cliente.

### D3 — Banco: PostgreSQL com isolamento por tenant via RLS
**Escolha:** PostgreSQL (open-source, sem custo de licença por tenant, JSONB, particionamento, **Row-Level Security**). **Multi-tenancy** (ver D4). **Alternativa:** SQL Server (o cliente usa) — viável, mas licenciamento encarece o modelo SaaS multi-tenant; PostgreSQL é o padrão de mercado para SaaS.

### D4 — Estratégia de multi-tenancy: híbrida
- **Pequeno/médio porte:** banco compartilhado + `tenant_id` em todas as tabelas + **RLS** (isolamento no nível do banco) — eficiência de custo.
- **Grande porte / exigência de isolamento:** **schema-per-tenant** ou banco dedicado.
**Por quê:** equilibra custo (maioria dos municípios é pequena) e isolamento (entes maiores e exigências de auditoria). Tenant resolvido pelo *claim* do token (Keycloak) e propagado por filtro/contexto.

### D5 — IAM: Keycloak
OIDC/OAuth2, *realms*/grupos para perfis (servidor, gestor, RH, controladoria, tenant-admin, super-admin), MFA para perfis administrativos, e **federação gov.br** opcional. **Alternativa:** Spring Authorization Server (mais código próprio) ou Auth0/Cognito (custo recorrente, dado fora do país a avaliar).

### D6 — Registro offline-first
App grava marcações localmente (fila cifrada em SQLite) com **NSR** provisório e *hash* encadeado; sincroniza ao reconectar; servidor consolida NSR definitivo e detecta duplicidade por *idempotency key*. **Por quê:** conectividade rural. **Trade-off:** relógio do dispositivo não confiável → mitigação: carimbo do servidor + tolerância + marcação de "registro offline" auditável.

### D7 — Biometria facial on-device + verificação opcional no servidor
*Liveness*/detecção facial no dispositivo (ML Kit) e *template* comparado contra referência cadastrada; **imagem minimizada** (armazenar *embedding*/hash, não a foto crua sempre que possível). **Por quê:** privacidade (LGPD — dado sensível), funciona offline, reduz tráfego. **Alternativa:** serviço gerenciado (AWS Rekognition) — maior acurácia, porém custo por chamada, dependência de rede e exposição de imagem. Suportado como modo premium configurável.

### D8 — Conformidade Portaria 671/2021 (REP-P)
Geração de **AFD** (Arquivo Fonte de Dados) e **AEJ** (Arquivo Eletrônico de Jornada), **NSR sequencial imutável**, **comprovante de registro** ao trabalhador e integridade por **assinatura digital** (certificado ICP-Brasil do tenant). Aplicado a vínculos celetistas; estatutários seguem o estatuto do ente + IN 008/2021.

### D9 — Infra: contêineres + Kubernetes gerenciado em região Brasil
Docker + Kubernetes (EKS/GKE) — ou ECS/Cloud Run para começar mais simples — em **sa-east-1 (São Paulo)** ou equivalente. IaC com **Terraform**. CI/CD no **GitLab**. **Por quê:** residência de dados (LGPD), elasticidade por carga de batimento (picos no início/fim de expediente), HA e DR.

### D10 — Assinatura eletrônica do espelho
Ciência do servidor por assinatura eletrônica (login + OTP/biometria) com registro de evidências (data, IP, dispositivo, hash do documento); fechamento bloqueia edição. Suporte futuro a assinatura ICP-Brasil avançada.

## Stack (resumo)

| Camada | Tecnologia | Observação |
|---|---|---|
| Mobile | **Flutter** (Dart) | offline (Drift/SQLite), câmera, ML Kit, FCM push |
| Web | **React + TypeScript** (Vite), design system | painel RH/gestor/controladoria; Totem como PWA |
| Backend | **Java 21 + Spring Boot 3** (Spring Modulith) | REST/OpenAPI, Spring Security (OIDC) |
| Banco | **PostgreSQL 16** | RLS multi-tenant, particionamento de marcações |
| Cache/sessão | **Redis** | cache, rate-limit, locks |
| Mensageria | **RabbitMQ** | sync offline, notificações, integrações |
| IAM | **Keycloak** | OIDC, MFA, federação gov.br |
| Armazenamento | **S3-compatível** | fotos, atestados, AFD/AEJ (cifrado) |
| Observabilidade | **Prometheus + Grafana + Loki + Sentry** | métricas, logs, erros |
| Infra | **Docker + Kubernetes + Terraform** | região Brasil |
| CI/CD | **GitLab CI** | build, testes, scan SAST/dep, deploy |
| Push/E-mail | **Firebase Cloud Messaging / SES** | notificações |

## Infraestrutura e ambientes

- **Ambientes:** `dev` → `staging` (homologação) → `prod`, isolados; dados de prod nunca em dev.
- **Alta disponibilidade:** múltiplas zonas, réplicas do PostgreSQL (leitura), autoscaling do backend.
- **Backup/DR:** *point-in-time recovery* do banco, backups cifrados versionados em S3, RPO ≤ 15 min / RTO ≤ 4 h (a validar), *runbook* de recuperação.
- **Segurança de rede:** WAF + API Gateway, TLS 1.2+, *secrets* em cofre (Vault/SSM), redes privadas para banco/fila.
- **Escalabilidade:** marcações particionadas por competência/tenant; picos de início/fim de expediente absorvidos por fila + autoscaling.

## Segurança e LGPD

- **Base legal:** cumprimento de obrigação legal e exercício regular de competência do poder público (controle de frequência); registro de **consentimento** específico para biometria quando exigido.
- **Dado sensível (biometria):** minimização (preferir *embedding*/hash à imagem), cifragem em repouso e trânsito, acesso segregado e auditado, **DPIA/RIPD** (relatório de impacto), prazos de **retenção e descarte**.
- **Direitos do titular:** portais/rotas para acesso, correção e eliminação; encarregado (**DPO**) por contrato.
- **Trilhas de auditoria** imutáveis (quem registrou/alterou o quê, quando, de onde) — atende IN 008/2021 e Portaria 671.
- **Hardening:** RBAC/ABAC, MFA administrativo, *least privilege*, SAST/DAST e *pentest* antes do go-live, gestão de vulnerabilidades de dependências.
- **Acessibilidade:** web aderente a **WCAG 2.1 AA / eMAG**; app seguindo diretrizes de acessibilidade das plataformas (LBI).

## Estrutura de repositórios

```
ponto-eletronico/
├── backend/        # Spring Boot modular (Maven multi-módulo)
│   └── modules: iam, tenant, cadastro, jornada, registro, apuracao,
│                banco-horas, abono, espelho, relatorios, auditoria,
│                integracao, billing, notificacao
├── mobile/         # Flutter (app servidor/gestor)
├── web/            # React + TS (painel RH/gestor/controladoria + totem PWA)
├── infra/          # Terraform, manifests k8s/helm, pipelines
└── docs/           # ADRs, OpenAPI, conformidade (IN-008, P671, LGPD)
```

## Custos (ordens de grandeza, a refinar)

- **Lojas:** Apple Developer US$ 99/ano; Google Play US$ 25 (único).
- **Infra cloud (início, multi-tenant pequeno/médio):** faixa de **R$ 3k–8k/mês** (cluster gerenciado + Postgres HA + storage + observabilidade), crescendo com a base.
- **Serviços opcionais:** reconhecimento facial gerenciado (por chamada), SMS/OTP, certificado ICP-Brasil.
- Modelo de receita sugerido: assinatura por **servidor ativo/mês** ou por faixa de servidores por ente.

## Equipe sugerida

PO/PM • Tech Lead • 2–3 devs Backend (Java/Spring) • 2 devs Mobile (Flutter) • 1–2 devs Web (React) • UX/UI • QA/automação • DevOps/SRE • apoio Jurídico/DPO (LGPD). Papéis acumuláveis em time enxuto inicial.

## Cronograma (macro, indicativo)

- **F0 – Fundações (4–6 sem.):** infra/IaC, IAM/multi-tenant, esqueleto backend/web/mobile, CI/CD.
- **F1 – Núcleo de ponto (6–8 sem.):** cadastro, jornadas/escalas, **registro mobile/web offline + geo + facial**, espelho.
- **F2 – Apuração (5–7 sem.):** ocorrências, banco de horas, abonos/justificativas + aprovação.
- **F3 – Fechamento & conformidade (4–6 sem.):** assinatura/fechamento, AFD/AEJ, auditoria, relatórios/dashboards.
- **F4 – Integrações & SaaS (4–6 sem.):** folha, eSocial, billing/onboarding, publicação nas lojas.
- **F5 – Hardening & go-live (3–4 sem.):** LGPD/DPIA, pentest, performance, piloto em 1 ente, produção.

## Risks / Trade-offs

- **Conectividade rural** → offline-first com fila cifrada e sync idempotente; carimbo do servidor.
- **Relógio do dispositivo adulterável** → tolerância + flag "offline" auditável + correção no servidor.
- **Rejeição/risco LGPD da biometria** → consentimento, minimização (embedding), modo alternativo (foto+geo) e DPIA; transparência com servidor/sindicato.
- **Integração com folhas legadas heterogêneas** → camada de conectores + exportação por arquivo padrão (CSV/posicional/AFD) como *fallback*.
- **Isolamento multi-tenant** → RLS + testes automatizados de vazamento entre tenants; schema/banco dedicado para entes sensíveis.
- **Acurácia facial / falso negativo** → *fallback* supervisionado (gestor) e segundo fator; métricas de qualidade.
- **Adoção e troca de gestão municipal** → onboarding assistido, contratos plurianuais, exportação de dados garantida (anti-aprisionamento).

## Migration Plan

Produto novo — sem migração de dados legada obrigatória. Estratégia de **go-live por piloto**: 1 ente em produção assistida → ajustes → expansão. *Rollback* por versionamento de release (blue/green ou *canary* no Kubernetes) e *feature flags*. Importação inicial de servidores via planilha/integração com a folha do ente.

## Open Questions

- Quais **sistemas de folha** dos primeiros clientes (define os primeiros conectores)?
- Proporção **estatutários × celetistas** (define peso do REP-P/Portaria 671)?
- **Totem/quiosque** é requisito de F1 ou pode ficar para F2?
- Reconhecimento facial: **on-device** é suficiente ou haverá exigência de serviço gerenciado por acurácia?
- Provedor de nuvem alvo (AWS/GCP/Azure) e exigências contratuais de **residência/soberania** de dados do setor público.
- Necessidade de **certificado ICP-Brasil** por ente para assinatura dos arquivos/espelho.
