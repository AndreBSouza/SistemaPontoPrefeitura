# Handoff — Ponto Municipal (continuar a construção da camada de UI/UX)

> Cole o conteúdo do bloco **"PROMPT PARA A NOVA SESSÃO"** (abaixo) na primeira mensagem
> da nova sessão do Claude Desktop. A máquina é a mesma (Windows, usuário `bs-an`),
> só muda a conta do Claude — todos os caminhos continuam válidos.

---

## PROMPT PARA A NOVA SESSÃO

Você vai continuar um projeto já existente nesta máquina: **Ponto Municipal**, um SaaS
multi-tenant de **ponto eletrônico / controle de frequência de servidores públicos
municipais** (app mobile Android/iOS, web para RH/gestor/controladoria, e totem).
Âncoras regulatórias: **IN 008/2021 TCM-GO** (controle interno exige controle de
frequência), modo **REP-P (Portaria MTP 671/2021)** e conformidade **LGPD**.

Diretório do projeto: `C:\Users\bs-an\Documents\ProjetoP`
Comece **lendo** estes arquivos para se situar:
- `C:\Users\bs-an\Documents\ProjetoP\HANDOFF.md` (este arquivo)
- `openspec/changes/ponto-eletronico-municipal/` (proposal.md, design.md, specs/, tasks.md)
- `CLAUDE.md` e a pasta de memória em `C:\Users\bs-an\.claude\projects\C--Users-bs-an-Documents-ProjetoP\memory\`

### Restrições inegociáveis
1. **Isolamento total** do projeto vizinho `new-public-management`: NÃO tocar nele nem no
   git que ele usa. Trabalhe apenas dentro de `ProjetoP`.
2. **SOLID / Clean Code / DDD pragmáticos** — sem over-engineering, sem framework novo
   desnecessário (nada de MapStruct, hexagonal completo ou event-sourcing para CRUD).
   Padrões já no código: regra em POJO de domínio testável, comportamento em entidade/enum,
   erros via `ApiExceptionHandler` (ConflitoException→409, RecursoNaoEncontradoException→404,
   IllegalArgumentException→400). Mapeamento entidade→DTO via `static from(...)` no record.
3. **iOS também** faz parte do escopo (código + CI macOS já preparados; o binário precisa de Mac).

### Stack e estrutura (já existente e funcionando)
- **Backend**: Java 21, Spring Boot 3.3.5 (Maven) em `backend/`, package-by-feature
  `br.gov.ponto.*`. Spring Data JPA, Flyway (migrations V1..V13+), springdoc OpenAPI,
  micrometer/prometheus. Segurança: OAuth2 resource server (Keycloak OIDC/JWT, roles do
  realm → ROLE_*). Multi-tenancy: `TenantContext` (ThreadLocal) + filtragem por tenant na
  aplicação (mecanismo principal) + RLS no Postgres (defesa em profundidade).
- **Web**: React + TypeScript + Vite em `web/` (react-router-dom; PWA com /totem).
- **Mobile**: Flutter em `mobile/` (dio, uuid, connectivity_plus, geolocator, camera,
  google_mlkit_face_detection). Flutter SDK em `C:\Users\bs-an\dev\flutter`,
  Android SDK em `C:\Users\bs-an\Android\Sdk` (emulador AVD chamado `ponto`, API 35).
- **Testes**: `mvn -q -B -f backend/pom.xml test` (Postgres embarcado Zonky; ~43 testes
  devem ficar verdes). `cd web && npm run build`. `flutter analyze` em `mobile/`.

### Estado atual — o MOTOR está pronto, falta a CARA do produto
- **Backend rico** (20 controllers): tenant (prefeitura), cadastro (servidor/vínculo/lotação),
  jornada/escala, registro de ponto (NSR sequencial, idempotência), apuração + **justificativa
  (= atestado)**, **banco de horas**, **espelho/fechamento**, auditoria, relatórios/conformidade,
  **LGPD**, saas (onboarding/billing), **integração folha/eSocial**, notificação, biometria.
  → **A lógica de folha, atestado, banco de horas, espelho etc. JÁ EXISTE no backend.**
- **Web**: só `HomePage` + `TotemPage` (telas cruas). **NÃO existe painel administrativo.**
- **Mobile**: só `registro_page.dart` (uma tela com 4 botões ENTRADA/SAÍDA/INTERVALO).
- **Lotação** (`cadastro/domain/Lotacao.java`) existe mas é mínima (nome, sigla, chefia) —
  **não carrega regras de ponto/escala**.
- **Registro**: hoje o **cliente envia o `tipo`** (ENTRADA/SAÍDA/...). Não há dedução no servidor.

### O QUE CONSTRUIR (a camada que falta) — objetivo desta empreitada
Aplicação **completa, bonita/polida e ao mesmo tempo simples**, acessível para **usuários
idosos** usarem tranquilamente. Decisões de produto já tomadas (seguir):

1. **Botão único de ponto**: o servidor toca **UM** botão grande "Registrar ponto"; o
   **servidor (backend) deduz** se é entrada / início-intervalo / fim-intervalo / saída pela
   sequência de batidas do dia + a jornada do órgão, e responde em letras grandes
   (ex.: "Entrada registrada às 08:03"). NÃO expor botões separados de entrada/saída/intervalo.
   → Criar endpoint **`POST /api/registros/bater`** (infere o tipo; mantém NSR e idempotência).
2. **Acessibilidade (idosos)**: fontes grandes, alto contraste, alvos de toque amplos,
   linguagem simples, pouca informação por tela, feedback claro (sucesso verde / erro).
   Usar o **Design System gov.br (DSGov)** como base visual (oficial, acessível, familiar).
3. **Órgão de 1ª classe**: promover "Lotação" para **Órgão/Unidade** com **regras próprias**:
   jornada padrão, tolerância, política de banco de horas, geofence (lat/long/raio). Servidor
   lotado **herda** as regras do órgão (com override por vínculo quando necessário).

**Entregáveis:**
- **App do servidor (Flutter, acessível)**: botão único de ponto → biometria facial (liveness);
  **Meu espelho**, **Meus comprovantes**, **Solicitar atestado/justificativa** (com anexo),
  **Saldo de banco de horas**, **Notificações**, login gov.br. Redesenho visual polido.
- **Painel administrativo (web React, DSGov)**: navegação/layout + páginas:
  **Prefeituras** → **Órgãos** (com regras próprias de ponto/escala) → **Servidores/vínculos**
  → **Jornadas/Escalas**; **Atestados** (aprovar/recusar), **Espelho/Fechamento**,
  **Banco de horas**, **Relatórios/Conformidade (IN 008)**, **Folha (integração)**,
  **Auditoria**, **Billing**. Login Keycloak (OIDC).
- **Backend**: endpoint de **ponto inteligente** (dedução do tipo) + enriquecer **Órgão**
  (regras + herança de defaults para o vínculo).

**Forma de trabalho:** o usuário autorizou **orquestrar a equipe de agentes** para fazer
tudo em paralelo (backend + web + mobile), do mais lógico primeiro até terminar, já apontando
e implementando melhorias, mantendo os testes verdes a cada etapa. Atualize
`openspec/changes/ponto-eletronico-municipal/tasks.md` conforme avançar.

### Como rodar e testar localmente (perfil dev, sem Keycloak)
1. Subir o Postgres: `docker compose -f infra/docker-compose.yml up -d postgres`
   (precisa do **Docker Desktop ligado**).
2. Backend (API aberta no perfil dev; tenant via header `X-Tenant-Id`):
   `mvn -f backend/pom.xml spring-boot:run "-Dspring-boot.run.profiles=dev"`
   → no log, o `DevDataSeeder` imprime o ente demo: `X-Tenant-Id=...` e `vinculoId=...`
   (idempotente — reusa o ente "demo").
3. Web: `cd web && npm install && npm run dev` → http://localhost:5173 (e `/totem`).
4. Mobile no emulador `ponto`:
   - iniciar: `C:\Users\bs-an\Android\Sdk\emulator\emulator.exe -avd ponto -gpu auto`
   - rodar app: em `mobile/`, `flutter run --dart-define=API_TENANT=<X-Tenant-Id-do-log>`
     (o app fala com o backend em `10.0.2.2:8080`).
   - **Câmera no emulador**: a câmera virtual não tem rosto → o liveness não passa. Para testar
     a biometria, configurar a câmera frontal da AVD para a webcam do PC
     (`hw.camera.front=webcam0` em `~/.android/avd/ponto.avd/config.ini`) ou usar celular físico.

### Itens externos (dependem do usuário, não dá pra automatizar)
- Provisionar nuvem (AWS) com a conta dele (Terraform já em `infra/`).
- Credenciais gov.br (IdP) e build/publicação iOS (conta Apple + Mac).
- Reconhecimento facial 1:1 em produção, pentest, lojas, piloto e go-live.

Quando começar, confirme o entendimento em 2-3 linhas e siga construindo.

---

## Snapshot técnico (para referência rápida)
- Refactors SOLID da auditoria 1–9 já aplicados; resta só o #10 (baixo valor, manter).
- Perfil dev: `backend/src/main/resources/application-dev.yml` (`ponto.security.open=true`).
- Seeder: `backend/src/main/java/br/gov/ponto/dev/DevDataSeeder.java` (@Profile("dev")).
- Totem web tem campo de tenant; `PontoApi` (mobile) manda `X-Tenant-Id` via `--dart-define=API_TENANT`.
- Controllers REST existentes (base `/api`): tenants, servidores, jornadas, escalas, lotacoes,
  registros, apuracao, justificativas, banco-horas, espelho, auditoria, relatorios, conformidade,
  lgpd, onboarding, billing, integracoes, notificacoes, biometria, info.
- **Equipe de agentes** habilitada por `CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS=1` em
  `C:\Users\bs-an\.claude\settings.json` (settings de **usuário do Windows** — persiste ao
  trocar a conta do Claude). A orquestração usa as ferramentas Agent (subagentes) e Workflow.
