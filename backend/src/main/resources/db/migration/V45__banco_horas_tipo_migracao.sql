-- Migração zero-dor (12.5.3): permite lançamentos de saldo inicial (tipo MIGRACAO) no banco de horas.
ALTER TABLE banco_horas_lancamento DROP CONSTRAINT IF EXISTS banco_horas_lancamento_tipo_check;
ALTER TABLE banco_horas_lancamento ADD CONSTRAINT banco_horas_lancamento_tipo_check
    CHECK (tipo IN ('APURACAO', 'COMPENSACAO', 'AJUSTE', 'PRESCRICAO', 'MIGRACAO'));
