-- ============================================================================
-- TEMPLATE de particionamento de registro_ponto (NÃO está no path do Flyway).
-- Aplicar por um DBA, em janela de manutenção, VALIDANDO contra o schema/dados reais
-- (nomes de índices/políticas podem diferir do ambiente). Ver docs/particionamento-registro-ponto.md
-- ============================================================================

BEGIN;

-- 1) Renomeia a tabela atual.
ALTER TABLE registro_ponto RENAME TO registro_ponto_old;

-- 2) Recria como particionada por mês (a PK precisa conter a chave de partição).
CREATE TABLE registro_ponto (LIKE registro_ponto_old INCLUDING DEFAULTS)
    PARTITION BY RANGE (data_hora_servidor);
ALTER TABLE registro_ponto ADD PRIMARY KEY (id, data_hora_servidor);

-- 3) Partição "pega-tudo" (segurança) + a do mês corrente (replicar para os próximos).
CREATE TABLE registro_ponto_default PARTITION OF registro_ponto DEFAULT;
CREATE TABLE registro_ponto_2026_06 PARTITION OF registro_ponto
    FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');
-- ... criar as próximas faixas; automatizar com pg_partman.

-- 4) Copia os dados.
INSERT INTO registro_ponto SELECT * FROM registro_ponto_old;

-- 5) Reaplica RLS (multi-tenant) — vale para as partições.
ALTER TABLE registro_ponto ENABLE ROW LEVEL SECURITY;
ALTER TABLE registro_ponto FORCE ROW LEVEL SECURITY;
CREATE POLICY registro_ponto_tenant_isolation ON registro_ponto
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid);

-- 6) Índices de consulta (ajustar aos do ambiente).
CREATE INDEX ix_registro_ponto_tenant_nsr ON registro_ponto (tenant_id, nsr);
CREATE INDEX ix_registro_ponto_vinculo_data ON registro_ponto (vinculo_id, data_hora_servidor);

-- 7) Remove a tabela antiga após validar a cópia.
DROP TABLE registro_ponto_old;

COMMIT;
