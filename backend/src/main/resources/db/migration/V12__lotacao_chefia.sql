-- V12: chefia da lotacao (hierarquia de aprovacao)

alter table lotacao add column chefia_servidor_id uuid references servidor (id);
create index ix_lotacao_chefia on lotacao (tenant_id, chefia_servidor_id);
