-- V30: delegação de aprovação (12.6.8). No período, o delegado (substituto) vê e decide
-- as pendências de chefia do delegante (gestor titular), ex.: durante as férias.

create table delegacao_aprovacao (
    id                    uuid primary key,
    tenant_id             uuid not null references tenant (id),
    delegante_servidor_id uuid not null references servidor (id),
    delegado_servidor_id  uuid not null references servidor (id),
    data_inicio           date not null,
    data_fim              date not null,
    ativo                 boolean not null default true,
    criado_em             timestamptz not null default now()
);
create index ix_delegacao_delegado on delegacao_aprovacao (tenant_id, delegado_servidor_id, ativo);

alter table delegacao_aprovacao enable row level security;
alter table delegacao_aprovacao force row level security;
create policy rls_delegacao_aprovacao on delegacao_aprovacao
    using (tenant_id = current_setting('app.current_tenant', true)::uuid)
    with check (tenant_id = current_setting('app.current_tenant', true)::uuid);
