# Runbook — Go-Live

Checklist operacional para **colocar uma versão em produção** do Ponto Municipal:
migrations, smoke tests, feature flags e **rollback blue/green**.

> Estratégia: deploy **blue/green** com cutover por *weighted target group* / DNS.
> Ambiente: AWS sa-east-1. Banco PostgreSQL 16 (RLS por tenant). Auth OIDC/Keycloak.

---

## 1. Janela e papéis

- **Janela preferencial:** terça a quinta, 09:00–11:00 BRT (evitar sexta e fim de mês,
  fechamento de folha/frequência).
- **Release manager:** conduz o checklist e dá o go/no-go.
- **SRE on-call:** executa deploy e rollback.
- **DBA on-call:** acompanha migrations.
- **QA:** executa/valida smoke tests.

---

## 2. Pré-go-live (D-1)

- [ ] Tag/release criada a partir de `main`/`release/*`; changelog publicado.
- [ ] Pipeline CI verde (build, testes unit/integração, lint, SAST/dependências).
- [ ] **Pentest checklist** sem itens críticos em aberto (`docs/pentest-checklist.md`).
- [ ] Migrations revisadas: **compatíveis com a versão anterior** (expand/contract — ver §3).
- [ ] Backup full recente e **restore testado** (`docs/runbook-dr.md`).
- [ ] Segredos novos provisionados no SSM/Vault (`docs/secrets-management.md`) — nada em `.env`.
- [ ] Feature flags da release **desligadas** por padrão (rollout controlado depois).
- [ ] Plano de rollback impresso/aberto (esta página, §6).
- [ ] Comunicação aos entes piloto, se houver impacto perceptível.

---

## 3. Migrations de banco (expand / contract)

Regra de ouro do blue/green: **o banco precisa servir as duas versões ao mesmo tempo**
durante o cutover. Migrations destrutivas nunca acompanham o mesmo deploy do código que
deixa de usar a coluna.

Padrão **expand → migrate → contract**:
1. **Expand (este release):** adicionar colunas/tabelas/índices *nullable* e backfill assíncrono.
   Nada de `DROP`/`NOT NULL` em coluna ainda usada pela versão azul.
2. **Migrate:** código novo passa a ler/escrever a nova estrutura (atrás de feature flag se preciso).
3. **Contract (release seguinte, dias depois):** remover colunas/constraints antigas.

Execução:
- [ ] Rodar migrations **antes** de promover o green (ou como step inicial do deploy green).
- [ ] Criar índices com `CREATE INDEX CONCURRENTLY` (sem lock de tabela grande).
- [ ] Confirmar versão final do Flyway/Liquibase: sem migration `pending`/falha parcial.
- [ ] **Não** rebaixar versão de migration em rollback — rollback é de **código**, não de schema
      (por isso o schema precisa ser compatível com a versão azul).

---

## 4. Deploy blue/green

1. **Green up:** subir o novo stack (azul continua servindo 100% do tráfego).
2. Green aponta para o **mesmo banco** (já migrado em modo expand).
3. **Aquecimento:** health checks `actuator/health` e `actuator/health/readiness` verdes;
   conexões com Postgres/Redis/RabbitMQ/Keycloak/MinIO OK.
4. **Smoke tests no green** via endpoint interno (§5) — sem tráfego de usuário ainda.
5. **Canário:** mover **10%** do tráfego para o green; observar 10–15 min (§7).
6. **Cutover:** se métricas OK, ir para **100%** green. Azul fica de pé (standby) por ≥ 1 h.
7. **Decomissionar azul** após período de observação (manter para rollback rápido até lá).

---

## 5. Smoke tests (executar no green antes do tráfego)

Funcionais mínimos (todos os tenants de teste):
- [ ] `GET /api/info` e `GET /actuator/health` -> 200.
- [ ] **Login OIDC** (Keycloak): obtém token; `/api/me` retorna o usuário/tenant correto.
- [ ] **Multi-tenant/RLS:** usuário do tenant A **não** vê dados do tenant B (403/lista vazia).
- [ ] **Registrar marcação** (ponto) -> persiste e aparece no espelho.
- [ ] **Upload de atestado** -> armazenado no MinIO/S3, antivírus/validação OK.
- [ ] **Geração de espelho/relatório** -> retorna PDF/dados.
- [ ] **Webhook/fila** (RabbitMQ) -> evento de marcação consumido.
- [ ] Latência p95 dos endpoints-chave dentro do baseline.

Critério: **100% verde** para liberar tráfego. Qualquer falha -> não promover.

---

## 6. Rollback

Gatilhos para rollback imediato:
- Erro 5xx acima do baseline (ex.: > 1% por 5 min) ou pico de latência.
- Falha de autenticação/autorização (quebra de RLS, login OIDC).
- Smoke test crítico falhando após cutover.

Procedimento (rápido — minutos):
1. **Reverter tráfego para o azul** (weighted target group volta a 100% azul / DNS).
   O azul ainda está de pé exatamente para isso.
2. Confirmar azul saudável e smoke tests do azul OK.
3. **Não reverter o schema** (foi expand-only, é compatível). Se uma feature nova causou o
   problema, **desligar a feature flag** em vez de mexer no banco.
4. Comunicar incidente; abrir postmortem (≤ 5 dias úteis).
5. Se houve dado gravado pelo green em estrutura nova, validar consistência antes de reabrir.

> Se o problema for de **dados** (corrupção/migration ruim), seguir `docs/runbook-dr.md`
> (PITR) — rollback de tráfego não resolve corrupção de banco.

---

## 7. Feature flags

- Flags servem para **separar deploy de release**: código vai a produção desligado e é
  ativado gradualmente por tenant.
- Convenções:
  - Toda feature de risco entra atrás de flag, **default OFF**.
  - Rollout por tenant: ativar primeiro nos **entes piloto** (`docs/plano-piloto.md`).
  - Kill switch: toda flag deve poder ser desligada sem novo deploy.
- Limpeza: remover a flag (e o código morto) no release seguinte após 100% de adoção.

---

## 8. Pós-go-live (D0/D+1)

- [ ] Observabilidade 30 min pós-cutover: erros, latência, filas, conexões de banco.
- [ ] Validar jobs agendados (fechamento, exportação TCM-GO) na próxima execução.
- [ ] Novo backup full após estabilização.
- [ ] Atualizar status/changelog; comunicar entes que a release está estável.
- [ ] Agendar **contract migrations** para o release seguinte (§3).

> Documento vivo — revisar a cada release e quando a estratégia de deploy mudar.
