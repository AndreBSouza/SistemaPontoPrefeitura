-- V29: ausências programadas (férias/licenças, 12.4.1). No período a apuração trata os
-- dias como não úteis (não gera falta).

create table ausencia_programada (
    id          uuid primary key,
    tenant_id   uuid not null references tenant (id),
    vinculo_id  uuid not null references vinculo (id),
    tipo        varchar(24) not null
        check (tipo in ('FERIAS', 'LICENCA_MEDICA', 'LICENCA_MATERNIDADE',
                        'LICENCA_PATERNIDADE', 'LICENCA_PREMIO', 'LICENCA_NOJO', 'OUTRA')),
    data_inicio date not null,
    data_fim    date not null,
    observacao  varchar(300),
    criado_em   timestamptz not null default now()
);
create index ix_ausencia_vinculo on ausencia_programada (tenant_id, vinculo_id, data_inicio, data_fim);

alter table ausencia_programada enable row level security;
alter table ausencia_programada force row level security;
create policy rls_ausencia_programada on ausencia_programada
    using (tenant_id = current_setting('app.current_tenant', true)::uuid)
    with check (tenant_id = current_setting('app.current_tenant', true)::uuid);
