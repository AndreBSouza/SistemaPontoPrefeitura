# Ponto Municipal

SaaS multi-município de **ponto eletrônico / controle de frequência** para servidores
públicos municipais. App mobile (Android/iOS), web (RH, gestor, controladoria) e totem.

Ancorado na **IN 008/2021 do TCM-GO** (controle de frequências do Sistema de Controle
Interno), com suporte ao modo **REP-P da Portaria MTP 671/2021** e aderência à **LGPD**.

> A especificação completa (proposta, design, specs e plano) vive em
> [`openspec/changes/ponto-electronico-municipal`](openspec/changes/ponto-eletronico-municipal/).

## Estrutura do monorepo

```
.
├── backend/    # Java 21 + Spring Boot 3 (monólito modular) — API
├── web/        # React + TypeScript (Vite) — painel RH/gestor/controladoria
├── mobile/     # Flutter — app do servidor/gestor (requer Flutter SDK)
├── infra/      # docker-compose (dev local), IaC e configs
└── openspec/   # especificação dirigida (proposta/design/specs/tasks)
```

## Subir o ambiente de desenvolvimento

Pré-requisitos: Java 21+, Maven, Node 20+, Docker. (Flutter para o mobile.)

```bash
# 1) Infra local (PostgreSQL, Redis, RabbitMQ, Keycloak, MinIO)
docker compose -f infra/docker-compose.yml up -d

# 2) Backend (http://localhost:8080 — Swagger em /swagger-ui.html)
mvn -f backend/pom.xml spring-boot:run

# 3) Web (http://localhost:5173)
cd web && npm install && npm run dev
```

Verificação rápida do backend: `GET http://localhost:8080/api/info` e
`GET http://localhost:8080/actuator/health`.

## Status

Fundação em construção (ver progresso em
[`tasks.md`](openspec/changes/ponto-eletronico-municipal/tasks.md)).
