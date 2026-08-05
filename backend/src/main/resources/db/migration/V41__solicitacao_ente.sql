-- V41: solicitação pública de adesão de ente (self-service onboarding, 12.3.13). Tabela GLOBAL
-- (sem RLS, como a tabela tenant) — é pré-tenant. Só vira um tenant após aprovação do operador.

create table solicitacao_ente (
    id                uuid primary key,
    nome              varchar(120) not null,
    slug              varchar(60) not null,
    tipo_poder        varchar(20) not null,
    responsavel_nome  varchar(120) not null,
    responsavel_email varchar(160) not null,
    status            varchar(20) not null,
    tenant_id         uuid references tenant (id),
    motivo_decisao    varchar(300),
    criado_em         timestamptz not null default now(),
    decidido_em       timestamptz
);
create index ix_solicitacao_ente_status on solicitacao_ente (status, criado_em);
