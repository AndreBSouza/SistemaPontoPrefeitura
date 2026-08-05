-- V34: apropriação de horas por projeto/convênio/fonte (12.4.4) — apoia a prestação de contas.

create table projeto (
    id         uuid primary key,
    tenant_id  uuid not null references tenant (id),
    nome       varchar(200) not null,
    fonte      varchar(120),
    criado_em  timestamptz not null default now()
);
create index ix_projeto_tenant on projeto (tenant_id);

create table apropriacao_horas (
    id          uuid primary key,
    tenant_id   uuid not null references tenant (id),
    vinculo_id  uuid not null references vinculo (id),
    projeto_id  uuid not null references projeto (id),
    data        date not null,
    minutos     integer not null,
    descricao   varchar(300),
    criado_em   timestamptz not null default now()
);
create index ix_apropriacao_tenant_data on apropriacao_horas (tenant_id, data);

alter table projeto enable row level security;
alter table projeto force row level security;
create policy rls_projeto on projeto
    using (tenant_id = current_setting('app.current_tenant', true)::uuid)
    with check (tenant_id = current_setting('app.current_tenant', true)::uuid);

alter table apropriacao_horas enable row level security;
alter table apropriacao_horas force row level security;
create policy rls_apropriacao_horas on apropriacao_horas
    using (tenant_id = current_setting('app.current_tenant', true)::uuid)
    with check (tenant_id = current_setting('app.current_tenant', true)::uuid);
