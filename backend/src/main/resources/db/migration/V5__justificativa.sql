-- V5: abonos/justificativas com workflow de aprovacao

create table justificativa (
    id             uuid primary key,
    tenant_id      uuid not null references tenant (id),
    vinculo_id     uuid not null references vinculo (id),
    tipo           varchar(20) not null
        check (tipo in ('FALTA', 'ATRASO', 'SAIDA_ANTECIPADA', 'LICENCA', 'FERIAS', 'ATESTADO', 'OUTRO')),
    data_inicio    date not null,
    data_fim       date not null,
    motivo         varchar(500),
    anexo          varchar(300),
    status         varchar(12) not null check (status in ('PENDENTE', 'APROVADA', 'REJEITADA')),
    aprovador_id   uuid,
    decisao_em     timestamptz,
    motivo_decisao varchar(500),
    criado_em      timestamptz not null default now()
);
create index ix_justificativa_tenant on justificativa (tenant_id);
create index ix_justificativa_vinculo on justificativa (vinculo_id);

alter table justificativa enable row level security;
alter table justificativa force row level security;
create policy rls_justificativa on justificativa
    using (tenant_id = current_setting('app.current_tenant', true)::uuid)
    with check (tenant_id = current_setting('app.current_tenant', true)::uuid);
