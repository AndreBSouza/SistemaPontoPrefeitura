-- V10: plano de assinatura por ente (preco por servidor ativo/mes, em centavos)

create table plano (
    id                            uuid primary key,
    tenant_id                     uuid not null references tenant (id),
    preco_servidor_ativo_centavos integer not null,
    criado_em                     timestamptz not null default now(),
    constraint uq_plano_tenant unique (tenant_id)
);
create index ix_plano_tenant on plano (tenant_id);

alter table plano enable row level security;
alter table plano force row level security;
create policy rls_plano on plano
    using (tenant_id = current_setting('app.current_tenant', true)::uuid)
    with check (tenant_id = current_setting('app.current_tenant', true)::uuid);
