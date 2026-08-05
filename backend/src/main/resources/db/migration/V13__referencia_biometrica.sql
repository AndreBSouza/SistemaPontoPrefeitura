-- V13: referencia biometrica do servidor (enrollment), gravada apos consentimento LGPD.
-- Guardar preferencialmente template/hash (minimizacao), nao a imagem crua.

create table referencia_biometrica (
    id          uuid primary key,
    tenant_id   uuid not null references tenant (id),
    servidor_id uuid not null references servidor (id),
    referencia  varchar(500) not null,
    criado_em   timestamptz not null default now(),
    constraint uq_referencia_servidor unique (servidor_id)
);
create index ix_referencia_tenant on referencia_biometrica (tenant_id);

alter table referencia_biometrica enable row level security;
alter table referencia_biometrica force row level security;
create policy rls_referencia on referencia_biometrica
    using (tenant_id = current_setting('app.current_tenant', true)::uuid)
    with check (tenant_id = current_setting('app.current_tenant', true)::uuid);
