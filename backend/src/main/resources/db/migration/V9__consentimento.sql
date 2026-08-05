-- V9: registro de consentimento LGPD (ex.: biometria)

create table consentimento (
    id            uuid primary key,
    tenant_id     uuid not null references tenant (id),
    servidor_id   uuid not null references servidor (id),
    finalidade    varchar(40) not null,
    concedido     boolean not null,
    registrado_em timestamptz not null default now()
);
create index ix_consentimento_tenant on consentimento (tenant_id);
create index ix_consentimento_servidor on consentimento (servidor_id);

alter table consentimento enable row level security;
alter table consentimento force row level security;
create policy rls_consentimento on consentimento
    using (tenant_id = current_setting('app.current_tenant', true)::uuid)
    with check (tenant_id = current_setting('app.current_tenant', true)::uuid);
