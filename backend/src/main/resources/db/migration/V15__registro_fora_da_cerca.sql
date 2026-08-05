-- V15: marca de batida fora da cerca geografica do orgao (geofencing).
-- Registro imutavel; "fora da area" e sinalizado para tratamento (nao bloqueia).

alter table registro_ponto add column fora_da_cerca boolean not null default false;
