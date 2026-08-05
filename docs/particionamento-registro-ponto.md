# Particionamento de `registro_ponto`

## Por que
`registro_ponto` é a tabela que mais cresce (uma batida por marcação, para sempre). Em volumes
altos (milhões de linhas), particionar melhora desempenho de consultas por período/competência e
facilita arquivamento/retenção.

## Por que **não** está numa migration automática do Flyway
Particionar é uma operação de **ops/DBA**, não um simples `ALTER`:
- O PostgreSQL exige que a **chave de partição entre na PK** — a PK passa de `(id)` para
  `(id, data_hora_servidor)`.
- Converter uma tabela **com dados** exige recriar + copiar (janela de manutenção) ou `pg_partman`.
- É preciso uma **rotina que cria as partições futuras** (senão inserts futuros caem na default).
- A tabela tem **RLS** (multi-tenant) que precisa ser reaplicada na tabela particionada.

Forçar isso no Flyway quebraria a base existente e/ou a suíte. Então entregamos a **estratégia +
um template validado** ([db/manual/particionamento_registro_ponto.sql](../backend/src/main/resources/db/manual/particionamento_registro_ponto.sql))
para o DBA aplicar com segurança.

## Estratégia recomendada
- **Chave:** `RANGE (data_hora_servidor)`, partições **mensais** (ou anuais em entes pequenos).
- **Partição `DEFAULT`** como rede de segurança (pega o que não casar com nenhuma faixa).
- **Idempotência:** a verificação continua na aplicação (`findByTenantIdAndIdempotencyKey`); se
  quiser a garantia também no banco, o índice único passa a incluir `data_hora_servidor`
  (unicidade por partição) — avaliar com o DBA.
- **RLS:** habilitar/forçar na tabela particionada (as políticas valem para as partições).
- **Manutenção:** usar **pg_partman** (ou um cron) para criar as partições dos próximos meses e
  aplicar política de retenção/arquivamento.

## Passos (resumo)
1. Janela de manutenção + backup.
2. Aplicar o template (recria como particionada, cria default + mês corrente, copia os dados,
   reaplica RLS e índices).
3. Validar contagem de linhas e `mvn test` / consultas-chave.
4. Instalar `pg_partman` e agendar a criação automática das partições futuras.
5. Monitorar planos de consulta (partition pruning) e ajustar o intervalo se preciso.
