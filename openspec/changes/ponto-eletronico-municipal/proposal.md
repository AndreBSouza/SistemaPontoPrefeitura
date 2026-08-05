## Why

Prefeituras e câmaras municipais precisam controlar a frequência dos servidores de forma confiável e auditável. Hoje muitos entes ainda usam livro/folha de ponto em papel, o que gera fraude (registro por terceiros — "buddy punching"), retrabalho na apuração, divergência com a folha de pagamento e **não-conformidade com o Controle Interno** exigido pela **IN 008/2021 do TCM-GO** (Anexo 1, item 2 "Atos de Pessoal", alínea *d*: *"verificar a existência de controles de frequências, arquivos e prontuários atualizados e organizados"*). Faltam ainda controles para servidores em campo (saúde, educação, obras, zeladoria) e em localidades com internet instável.

**Oportunidade:** um SaaS **multi-município** de ponto eletrônico, com **app mobile** (registro em campo, offline, biometria facial e geolocalização) e **web** (RH, chefias e Controladoria Interna), que cumpre a IN 008/2021, opera no modo **REP-P** da **Portaria MTP 671/2021** para celetistas e é aderente à **LGPD**. Receita recorrente por assinatura, com baixo custo de adoção para o município.

## What Changes

Produto novo (greenfield). Entregas:

- **App mobile Android/iOS** (servidor e gestor): registro de ponto com *geofencing*, reconhecimento facial com prova de vida (*liveness*), **modo offline com sincronização**, comprovante de registro (NSR), espelho de ponto, solicitação de abono/justificativa com anexo (atestado), consulta de escala e notificações push.
- **Web desktop** (RH/Departamento de Pessoal, chefias, Controladoria): cadastro de servidores e vínculos, jornadas/escalas, apuração de marcações, banco de horas, workflow de aprovação, fechamento e espelho assinado, relatórios/dashboards, trilha de auditoria e exportação para folha.
- **Plataforma SaaS multi-tenant**: isolamento de dados por ente, onboarding, perfis/permissões, planos e faturamento (billing), administração central do operador.
- **Conformidade**: IN 008/2021 (controle de frequência do SCI); Portaria MTP 671/2021 (REP-P — geração de **AFD/AEJ**, NSR imutável, comprovante ao trabalhador, integridade por assinatura); **LGPD** (biometria/geolocalização como dados pessoais/sensíveis, base legal de cumprimento de obrigação legal e exercício de poder público, registro de consentimento, retenção e descarte); assinatura eletrônica do espelho.
- **Integrações**: sistemas de folha de pagamento municipais, **eSocial** (eventos de jornada quando aplicável), login **gov.br** (opcional).
- **BREAKING**: N/A (produto novo, sem base instalada).

## Capabilities

### New Capabilities
- `multi-tenancy-iam`: isolamento por ente (tenant), autenticação, perfis e permissões (RBAC), sessão, federação opcional gov.br.
- `cadastro-servidor-vinculo`: cadastro de servidores, vínculos/matrículas, regimes (estatutário/celetista/comissionado), lotações e organograma do ente.
- `jornada-escala`: definição de jornadas (fixa, flexível, 12x36, plantão, carga horária de magistério), escalas, turnos e intervalos.
- `registro-ponto`: registro multicanal (mobile, web, totem), offline-first, geolocalização/geofencing, biometria facial + liveness, NSR imutável e comprovante.
- `apuracao-frequencia`: tratamento de marcações, ocorrências (atraso, falta, hora extra, adicional noturno), banco de horas e compensação, abonos/justificativas com workflow de aprovação.
- `espelho-fechamento-assinatura`: espelho mensal, ciência/assinatura eletrônica do servidor, fechamento de competência e bloqueio de edição.
- `relatorios-auditoria-conformidade`: relatórios e dashboards de RH/frequência, trilha de auditoria imutável, geração AFD/AEJ (Portaria 671), evidências de conformidade IN 008/2021 e controles LGPD.
- `plataforma-saas-integracoes`: onboarding de tenant, planos/assinatura e faturamento, notificações (push/e-mail), integrações com folha de pagamento e eSocial.

### Modified Capabilities
- (nenhuma — produto novo, sem specs existentes em `openspec/specs/`)

## Impact

- **Stacks propostas** (detalhadas em `design.md`): Backend **Java 21 + Spring Boot 3.x** (monólito modular evoluindo p/ serviços); Mobile **Flutter** (alternativa: React Native); Web **React + TypeScript**; **PostgreSQL**; **Keycloak** (IAM/multi-realm); **Redis**; **RabbitMQ**; *object storage* (S3) p/ fotos e atestados; **Docker/Kubernetes**; **Terraform**; **GitLab CI/CD**; hospedagem em **região Brasil** (LGPD).
- **Novos artefatos**: repositórios backend, mobile, web e infra (IaC); pipelines de build/entrega; contas e fichas técnicas nas lojas (Apple App Store, Google Play).
- **Sistemas externos**: integração com sistemas de folha municipais (heterogêneos), eSocial e, opcionalmente, gov.br.
- **Riscos principais**: conectividade no interior (mitigado por *offline-first*); aceitação e consentimento de biometria (LGPD/sindicatos); integração com folhas legadas diversas; segurança de dados sensíveis (biometria) exigindo cifragem e governança; sazonalidade de troca de gestão municipal.
