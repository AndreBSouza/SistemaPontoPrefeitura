-- V1: multi-tenancy (tenant) + cadastro de servidores e vinculos
-- Isolamento por RLS (defesa em profundidade), complementando o filtro de aplicacao.

create table tenant (
    id          uuid primary key,
    nome        varchar(200) not null,
    slug        varchar(60)  not null unique,
    tipo_poder  varchar(20)  not null check (tipo_poder in ('EXECUTIVO', 'LEGISLATIVO')),
    ativo       boolean      not null default true,
    criado_em   timestamptz  not null default now()
);

create table lotacao (
    id          uuid primary key,
    tenant_id   uuid not null references tenant (id),
    nome        varchar(200) not null,
    sigla       varchar(30),
    criado_em   timestamptz  not null default now()
);
create index ix_lotacao_tenant on lotacao (tenant_id);

create table servidor (
    id          uuid primary key,
    tenant_id   uuid not null references tenant (id),
    cpf         varchar(11)  not null,
    nome        varchar(200) not null,
    email       varchar(200),
    ativo       boolean      not null default true,
    criado_em   timestamptz  not null default now(),
    constraint uq_servidor_tenant_cpf unique (tenant_id, cpf)
);
create index ix_servidor_tenant on servidor (tenant_id);

create table vinculo (
    id                    uuid primary key,
    tenant_id             uuid not null references tenant (id),
    servidor_id           uuid not null references servidor (id),
    matricula             varchar(30) not null,
    regime                varchar(20) not null check (regime in ('ESTATUTARIO', 'CELETISTA', 'COMISSIONADO')),
    cargo                 varchar(150),
    carga_horaria_semanal integer,
    lotacao_id            uuid references lotacao (id),
    data_admissao         date,
    ativo                 boolean not null default true,
    criado_em             timestamptz not null default now(),
    constraint uq_vinculo_tenant_matricula unique (tenant_id, matricula)
);
create index ix_vinculo_tenant on vinculo (tenant_id);
create index ix_vinculo_servidor on vinculo (servidor_id);

-- Row-Level Security por tenant. FORCE para valer inclusive ao dono da tabela.
alter table lotacao  enable row level security;
alter table servidor enable row level security;
alter table vinculo  enable row level security;
alter table lotacao  force row level security;
alter table servidor force row level security;
alter table vinculo  force row level security;

-- current_setting(..., true) retorna NULL se a GUC nao estiver definida -> nenhuma linha.
create policy rls_lotacao on lotacao
    using (tenant_id = current_setting('app.current_tenant', true)::uuid)
    with check (tenant_id = current_setting('app.current_tenant', true)::uuid);
create policy rls_servidor on servidor
    using (tenant_id = current_setting('app.current_tenant', true)::uuid)
    with check (tenant_id = current_setting('app.current_tenant', true)::uuid);
create policy rls_vinculo on vinculo
    using (tenant_id = current_setting('app.current_tenant', true)::uuid)
    with check (tenant_id = current_setting('app.current_tenant', true)::uuid);
