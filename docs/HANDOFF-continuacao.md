# Prompt de continuação — Ponto Municipal (cole na nova sessão do Claude)

Você está assumindo o desenvolvimento do **Ponto Municipal**, um SaaS **multi-tenant** de ponto
eletrônico para **servidores públicos municipais**. Stack: **backend** Java 21 / Spring Boot 3.3.5
/ Maven (`br.gov.ponto.*`, package-by-feature); **web** React + TS + Vite; **mobile** Flutter;
**infra** Terraform/Keycloak/docker-compose. Âncoras legais: **IN 008/2021 TCM-GO**, **REP-P
(Portaria MTP 671/2021)**, **LGPD**.

Ambiente: diretório `C:\Users\bs-an\Documents\ProjetoP` (Windows; shells PowerShell **e** Bash).
Flutter em `C:\Users\bs-an\dev\flutter\bin\flutter`. Não é repositório git.

## 1) Leia ISTO primeiro para se situar (a verdade está nos arquivos, não na memória)
- `openspec/changes/ponto-eletronico-municipal/tasks.md` — estado oficial. Seções **1–11** = entregue
  (11.16–11.19 são os incrementos recentes); **§12** = backlog comercial/produto a implementar.
- `docs/`: `passo-a-passo-implantacao.md`, `afd-assinatura-icp-brasil.md`,
  `particionamento-registro-ponto.md`, `stitch-prompt-mobile.md`, `stitch-prompt-web.md`.
- Memória do projeto (se existir nesta máquina):
  `C:\Users\bs-an\.claude\projects\C--Users-bs-an-Documents-ProjetoP\memory\MEMORY.md` e
  `proxima-fase-camada-ui-ux.md`.

## 2) Restrições invioláveis
- **Sem gov.br** (removido a pedido): servidor autentica por **código de ativação → token de
  dispositivo** (header `X-Device-Token`); admin pelo **Keycloak corporativo**.
- **NUNCA** tocar no projeto irmão `new-public-management` nem no git dele.
- **NÃO** implementar **anti-fraude facial na batida** nem **recadastramento/prova de vida**
  (excluídos a pedido — o framework facial existe, mas não é o foco).
- SOLID/Clean Code/DDD pragmático; não commitar segredos (keystore/key.properties são gitignored).
- **Manter os gates verdes** e trabalhar em **incrementos verificados**. Ler o arquivo antes de editar.

## 3) Estado atual verificado
- **Backend: 189 testes verdes**; web build+vitest 3; flutter analyze limpo + test 17. Migrations até **V43**.
  **L30 (2026-06-26) — subdomínio por ente (12.2.5, camada de app):** `Tenant.subdominio` (V43, único) +
  `PUT /api/branding/subdominio` + público `GET /api/publico/ente/{subdominio}` → identidade p/ autoconfig
  do app/login; campo no painel. `SubdominioTest`. (DNS/SMTP = infra à parte.)
  **Fix de segurança (HIGH, achado no review do app gestor):** `TenantFilter` deriva o tenant do
  PRINCIPAL autenticado (JWT ou token de dispositivo via `TenantAware`) e **ignora `X-Tenant-Id`
  quando autenticado** — fecha override cross-tenant por cabeçalho. + `forward-headers-strategy`.
  Regressão dedicada: **`TenantHeaderOverrideTest`** (device do ente A + `X-Tenant-Id: B` → branding de A).
  **L29 (2026-06-26) — logo no banco (12.2.1, sem S3):** `tenant_logo` (V42+RLS), `POST /api/branding/logo`
  + serve público `GET /api/publico/branding/{slug}/logo`; upload no painel; app resolve URL relativa. `LogoTest`.
  **L28 (2026-06-26) — app do gestor (12.3.11):** `GestorService` (vínculo→servidor→chefia) + `/api/me/chefia*`
  (resumo/pendentes/aprovar/rejeitar); só atua sobre o próprio time (403 senão) + alçada; tela "Gestão da
  equipe" no app (só p/ chefias). `GestorTest`. Review da L27 (onboarding) aplicado: `forward-headers-strategy`
  p/ rate-limit por IP atrás de proxy + rejeita slug já existente como tenant na solicitação.
  **L27 (2026-06-26) — onboarding self-service do ente (12.3.13):** público `POST /api/publico/onboarding`
  (rate-limit) grava solicitação PENDENTE (tabela global V41, sem RLS); operador aprova em
  `/api/onboarding/solicitacoes/{id}/aprovar` → provisiona tenant. Página pública `/aderir` + página
  do operador "Adesão de entes". `SolicitacaoEnteTest`.
  **L26 (2026-06-26) — hora-atividade/pisos (12.5.8):** `Jornada.horaAtividadeMin` (V40) + `PisoMagisterioService`
  + `GET /api/jornadas/piso-magisterio` (sinaliza jornadas abaixo de 1/3 da carga); campo no form de Jornadas
  + seção em Conformidade. `PisoMagisterioTest`.
  **L25 (2026-06-26) — sobreaviso (12.4.3):** módulo `sobreaviso` (V39+RLS), `GET/POST/DELETE /api/sobreaviso`
  (RH/chefia), horas à parte somadas por competência + nova coluna `sobreavisoMin` na folha; botão
  "⏰ Sobreaviso" por vínculo. Plantão já coberto por `TipoJornada.PLANTAO`/escala + 12×36. `SobreavisoTest`.
  **L24 (2026-06-26) — escala 12×36 (12.3.9):** `ApuracaoService.horarioEsperado` reconhece
  `ESCALA_12X36` — trabalha em dias alternados a partir da âncora (dataInicio da escala); folga sem
  falta, trabalho na folga vira HE. `RotacaoEscalaTest`. (Turno noturno cruzando meia-noite = futuro.)
  **L23 (2026-06-26) — trilha pessoal (12.1.2):** `GET /api/me/trilha` (`TrilhaService` agrega correções
  + justificativas do vínculo com a decisão, ordem decrescente) + tela "Meu histórico" no app. `TrilhaTest`.
  **L22 (2026-06-26) — multi-geofence (12.3.10):** cada órgão pode ter **várias áreas de referência**
  (locais volantes). Módulo `geofence_local` (V38 + RLS), `GET/POST/DELETE /api/lotacoes/{id}/locais`
  (admin), botão "📍 Locais" no painel. `RegistroService` combina a cerca primária + os locais: a
  batida só fica `foraDaCerca` quando está fora de **todas** (segue silencioso). `GeofenceLocalTest`.
  **L21 (2026-06-26) — geofence virou verificação SILENCIOSA só do admin:** a localização é definida
  pelo admin por órgão (opcional), **nunca bloqueia nem alerta o servidor**, e a localização +
  indicador "fora da área" aparecem no **registro de ponto** para o admin conferir. Removidos
  `geofenceBloqueia` (campo/coluna V37 + toggle web) e a ocorrência `FORA_DE_AREA` da apuração;
  `ComprovanteResponse` ganhou lat/lng; nova tela admin "📍 Localização" por vínculo (GET
  `/api/registros` → link Google Maps + badge "fora da área"); app não mostra mais aviso de fora-da-área
  (mantida só a detecção anti-fraude de mock-location). Gates: backend 156, web build+vitest 3, flutter 13.
  **§12: 47 itens FEITOS, 20 abertos** (os abertos são externos — IA, folha real, S3, REP-C,
  subdomínio, app gestor — ou exigem groundwork: rotação 12x36, hora-atividade,
  trilha por vínculo, multi-geofence, plantões/sobreaviso, onboarding ente).
  **Lote 16:** totem por matrícula (12.1.10) + branding na ativação (12.2.2). **L17:** transparência
  ativa (12.3.6). **L18:** escala em massa (12.6.3). **L19:** painel de ROI (12.3.1).
  **L20 (2026-06-26):** importação por órgão (CSV `lotacaoSigla`/`?lotacaoId` → lota e herda regras;
  corrigido o envio text/csv no web) + tema escuro (12.2.4) + teletrabalho (12.4.2) + alçadas de
  aprovação (12.6.13). Review adversarial (Workflow) confirmou 2 achados, já corrigidos: sigla única
  por ente (V36 + guard em LotacaoService) e textos #555/#444 ilegíveis no escuro (→ var(--color-gray-60)).
  **Lotes 7–15 (2026-06-25/26):** BI executivo + simulador/LRF (12.3.2/12.5.5/12.5.1),
  inconsistências (12.4.15), abonos exportável (12.6.15/12.6.6), adesão+isonomia (12.1.9/12.1.8),
  satisfação (12.1.3), diárias (12.4.6), dossiê/escudo TCM (12.1.7/12.5.6/12.3.4/12.3.5),
  carteira digital (12.3.8), bloqueio pós-fechamento (12.6.14), trilha antes-depois (12.6.16),
  lembretes (12.6.7), projetos/convênios (12.4.4), regimes estagiário/temporário/terceirizado (12.4.7),
  onboarding do servidor (12.6.5), conferência folha×frequência (12.6.12). **Lote 6:** 12.4.1, 12.6.8. **Lote 3 (2026-06-25):**
  12.1.1 modo adaptação, 12.3.7 comunicados oficiais, 12.5.2 caça fantasma/acúmulo (sem facial),
  12.6.1 aprovação/recusa em lote, 12.6.2 fechamento em lote + "o que falta fechar". **Lote 4
  (2026-06-25):** 12.4.5/12.1.6 calendário oficial + abono coletivo (neutraliza falta na apuração),
  12.6.10 alertas de risco (ajustes manuais + fora da cerca), 12.1.5 notificações "a seu favor".
  **Lote 5 (2026-06-25):** 12.1.4/12.6.4 correção de marcação ("esqueci de bater" servidor→chefia +
  correção do RH em lote; cria batida nova origem AJUSTE, encadeada — registros imutáveis). Corrigido
  um bug do verificador de integridade (12.6.9) com correções retroativas (janela não-contígua em NSR).
- **Web**: `npm run build` OK + `vitest` 3/3 (páginas novas: BI, Adesão, Projetos, Férias, Delegação,
  Comunicados, Correções, Calendário). **Mobile**: `flutter analyze` limpo + `flutter test` **13/13**
  (telas: Carteira, Minhas férias, Esqueci de bater, Avaliar, Comunicados). Migrations Flyway até
  **V34**. Testes usam Postgres embarcado (Zonky, refresh AFTER_EACH).

## 4) Gates (rode antes e depois de mexer)
- Backend: `mvn -q -B -f "C:/Users/bs-an/Documents/ProjetoP/backend/pom.xml" test`
  (conte em `target/surefire-reports/*.txt`. **Cuidado:** `| tail` mascara o exit code — use
  `; echo "exit=$?"` sem pipe, ou leia o arquivo de saída).
- Web: `npm --prefix "C:/Users/bs-an/Documents/ProjetoP/web" run build` e `... test`.
- Mobile (de `mobile/`): `"/c/Users/bs-an/dev/flutter/bin/flutter" analyze` e `... test`.

## 5) Já implementado (resumo — detalhe em tasks.md §11)
Botão único (`/api/me/bater`, dedução de tipo) · órgão com regras herdadas · geofence
(sinaliza×bloqueia) · tolerância/banco-horas/teto por órgão · **verificação configurável**
(`local_auth` digital/PIN/desenho + fallback facial; `/api/me/config`) · **hash-chain do NSR**
(V18) + **verificador de integridade** (`/api/relatorios/integridade`) · **AFD 671** (largura fixa,
`tenant.cnpj` V21) + **seam de assinatura ICP-Brasil** (`AssinaturaService`) · **espelho e
declaração em PDF** (OpenPDF; `/api/relatorios/espelho/pdf`, `/api/me/declaracao`) · **LGPD
autoatendimento** (`/api/me/lgpd/*`) · **rate-limit** na ativação · **cache** do device token ·
**cripto em repouso** (AES-GCM) da biometria + fila offline cifrada · **isolamento multi-tenant**
(RLS + testes) · **acesso web do servidor** (`/servidor`) · **white-label por prefeitura**
(`Branding` no Tenant V23 + `/api/branding` + `/api/me/branding`; painel "Identidade visual"; app
com tema reativo `AppTheme.themeFor`+`corPrimariaApp`) · **indicadores** (`/api/relatorios/indicadores`).

## 6) Próximo passo exato
1. Rode o gate (**151 verde**) ao assumir. A **maior parte do §12 já está `[x]`** (lotes 1–17).
   **Restam, codáveis aqui (mais complexos):** **12.6.3** templates de jornada/escala + aplicação em
   massa; **12.2.3** logo do ente nos documentos (PDF/AFD — imagem no OpenPDF); **12.2.4** tema escuro
   do painel (CSS); **12.4.2** teletrabalho (flag em `RegrasPonto` — geofence não aplica); **12.4.3**
   plantões/trocas + sobreaviso (troca-turno já existe); **12.3.9** escalas complexas 12x36 (rotação
   por data — refatora apuração); **12.5.8** pisos legais por categoria (exige modelar hora-atividade);
   **12.6.13** segregação/alçadas (exige o sub do JWT mapeado ao aprovador); **12.1.2** trilha pessoal
   "quem acessou meus dados" (exige auditoria indexada por vínculo); **12.3.13** onboarding self-service
   do ente (signup público — sensível); **12.3.11** app do gestor (mobile grande).
2. **Externos (codar mas não validável aqui — sinalizar):** integração de folha real (12.3.3),
   modelo de IA (12.3.12, 12.4.8–14), REP-C (12.5.3), upload S3 do logo (12.2.1), subdomínio/e-mail
   por ente (12.2.5), super-app c/ contracheque (12.5.4), dados de RCL/LRF, OIDC/Keycloak no ar, FCM,
   certificado ICP-Brasil, nuvem/lojas/piloto/go-live.

Continue de onde parei, mantendo o padrão: **ler antes de editar, gate verde a cada incremento,
atualizar `tasks.md` (§12) e a memória**.
