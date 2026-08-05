-- Idempotência da migração de saldo (12.5.3): no máximo um lançamento MIGRACAO por vínculo/ente.
-- Fecha a janela check-then-act — importações concorrentes/duplicadas não dobram o saldo (a 2ª viola
-- o índice e é tratada como "ignorada").
CREATE UNIQUE INDEX IF NOT EXISTS uq_banco_horas_migracao
    ON banco_horas_lancamento (tenant_id, vinculo_id)
    WHERE tipo = 'MIGRACAO';
