-- V24: modo adaptacao por orgao (12.1.1) — ate esta data o orgao so registra,
-- sem descontar/penalizar (atraso/falta/saida antecipada). Opcional (null = sem adaptacao).

alter table lotacao add column adaptacao_ate date;
