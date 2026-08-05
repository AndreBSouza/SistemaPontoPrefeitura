-- V6: banco de horas (ledger de lancamentos por vinculo; minutos +credito / -debito)

create table banco_horas_lancamento (
    id         uuid primary key,
    tenant_id  uuid not null references tenant (id),
    vinculo_id uuid not null references vinculo (id),
    data       date not null,
    minutos    integer not null,
    tipo       varchar(20) not null check (tipo in ('APURACAO', 'COMPENSACAO', 'AJUSTE', 'PRESCRICAO')),
    descricao  varchar(300),
    criado_em  timestamptz not null default now()
);
create index ix_banco_horas_tenant on banco_horas_lancamento (tenant_id);
create index ix_banco_horas_vinculo on banco_horas_lancamento (vinculo_id);

alter table banco_horas_lancamento enable row level security;
alter table banco_horas_lancamento force row level security;
create policy rls_banco_horas on banco_horas_lancamento
    using (tenant_id = current_setting('app.current_tenant', true)::uuid)
    with check (tenant_id = current_setting('app.current_tenant', true)::uuid);
