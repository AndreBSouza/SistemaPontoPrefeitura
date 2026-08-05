-- V39: sobreaviso (on-call, 12.4.3) — horas em que o servidor fica à disposição fora do
-- expediente. Contadas à parte (não afetam a apuração) e somadas por competência para a folha.

create table sobreaviso (
    id          uuid primary key,
    tenant_id   uuid not null references tenant (id),
    vinculo_id  uuid not null references vinculo (id),
    data        date not null,
    minutos     integer not null,
    observacao  varchar(200),
    criado_em   timestamptz not null default now()
);
create index ix_sobreaviso_vinculo on sobreaviso (tenant_id, vinculo_id, data);

alter table sobreaviso enable row level security;
alter table sobreaviso force row level security;
create policy rls_sobreaviso on sobreaviso
    using (tenant_id = current_setting('app.current_tenant', true)::uuid)
    with check (tenant_id = current_setting('app.current_tenant', true)::uuid);
