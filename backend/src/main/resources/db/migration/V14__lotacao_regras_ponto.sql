-- V14: Orgao de 1a classe — regras de ponto proprias da lotacao (orgao/unidade).
-- Jornada padrao, tolerancia, politica de banco de horas e geofence (lat/long/raio).
-- Todas as colunas sao opcionais; a heranca/defaults sao resolvidos na aplicacao.

alter table lotacao add column jornada_padrao_id      uuid references jornada (id);
alter table lotacao add column tolerancia_minutos     integer;
alter table lotacao add column banco_horas_habilitado boolean;
alter table lotacao add column geofence_latitude      numeric(10, 7);
alter table lotacao add column geofence_longitude     numeric(10, 7);
alter table lotacao add column geofence_raio_metros   integer;
