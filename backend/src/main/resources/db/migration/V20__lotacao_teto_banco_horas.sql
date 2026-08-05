-- V20: teto de acumulo do banco de horas por orgao (em minutos). null = usa o
-- default do sistema (200h). Permite ao ente reduzir/ampliar o limite legal.

alter table lotacao add column teto_banco_horas_minutos integer;
