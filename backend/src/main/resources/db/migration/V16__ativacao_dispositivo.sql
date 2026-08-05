-- V16: ativacao por codigo do RH + dispositivos (login unico por aparelho).
-- Sem RLS (como nsr_sequencia): a ativacao e a autenticacao por token ocorrem antes
-- de haver tenant no contexto e resolvem o ente pelo proprio segredo. O isolamento
-- por tenant nas listagens administrativas e feito por filtro de aplicacao (tenant_id explicito).

create table codigo_ativacao (
    id          uuid primary key,
    tenant_id   uuid not null references tenant (id),
    vinculo_id  uuid not null references vinculo (id),
    codigo_hash varchar(64) not null unique,
    expira_em   timestamptz not null,
    usado       boolean     not null default false,
    criado_em   timestamptz not null default now()
);
create index ix_codigo_ativacao_vinculo on codigo_ativacao (tenant_id, vinculo_id);

create table dispositivo (
    id          uuid primary key,
    tenant_id   uuid not null references tenant (id),
    vinculo_id  uuid not null references vinculo (id),
    nome        varchar(120),
    token_hash  varchar(64) not null unique,
    ativo       boolean     not null default true,
    criado_em   timestamptz not null default now()
);
create index ix_dispositivo_vinculo on dispositivo (tenant_id, vinculo_id);
