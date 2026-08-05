-- V2: jornadas de trabalho e escalas (atribuicao por vigencia ao vinculo)

create table jornada (
    id                        uuid primary key,
    tenant_id                 uuid not null references tenant (id),
    nome                      varchar(120) not null,
    tipo                      varchar(20)  not null
        check (tipo in ('FIXA', 'FLEXIVEL', 'ESCALA_12X36', 'PLANTAO', 'MAGISTERIO')),
    carga_horaria_semanal_min integer not null,
    tolerancia_min            integer not null default 0,
    intervalo_min             integer not null default 0,
    ativo                     boolean not null default true,
    criado_em                 timestamptz not null default now(),
    constraint uq_jornada_tenant_nome unique (tenant_id, nome)
);
create index ix_jornada_tenant on jornada (tenant_id);

create table escala (
    id           uuid primary key,
    tenant_id    uuid not null references tenant (id),
    vinculo_id   uuid not null references vinculo (id),
    jornada_id   uuid not null references jornada (id),
    data_inicio  date not null,
    data_fim     date,
    criado_em    timestamptz not null default now()
);
create index ix_escala_tenant on escala (tenant_id);
create index ix_escala_vinculo on escala (vinculo_id);

alter table jornada enable row level security;
alter table escala  enable row level security;
alter table jornada force row level security;
alter table escala  force row level security;

create policy rls_jornada on jornada
    using (tenant_id = current_setting('app.current_tenant', true)::uuid)
    with check (tenant_id = current_setting('app.current_tenant', true)::uuid);
create policy rls_escala on escala
    using (tenant_id = current_setting('app.current_tenant', true)::uuid)
    with check (tenant_id = current_setting('app.current_tenant', true)::uuid);
