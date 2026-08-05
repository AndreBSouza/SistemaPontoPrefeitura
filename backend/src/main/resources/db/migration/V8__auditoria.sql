-- V8: trilha de auditoria imutavel (somente insercao + leitura)

create table auditoria_evento (
    id          uuid primary key,
    tenant_id   uuid not null references tenant (id),
    acao        varchar(40) not null,
    entidade    varchar(40) not null,
    entidade_id varchar(64),
    ator        varchar(120),
    detalhe     varchar(1000),
    ocorrido_em timestamptz not null default now()
);
create index ix_auditoria_tenant on auditoria_evento (tenant_id);
create index ix_auditoria_entidade on auditoria_evento (tenant_id, entidade, entidade_id);

alter table auditoria_evento enable row level security;
alter table auditoria_evento force row level security;
create policy rls_auditoria on auditoria_evento
    using (tenant_id = current_setting('app.current_tenant', true)::uuid)
    with check (tenant_id = current_setting('app.current_tenant', true)::uuid);
