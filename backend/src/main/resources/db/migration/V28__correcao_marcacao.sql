-- V28: solicitações de correção de marcação (12.1.4 "esqueci de bater" + 12.6.4 correção do RH).
-- A correção aprovada gera uma NOVA marcação (registro_ponto origem AJUSTE), referenciada
-- em registro_id; registros existentes nunca são editados (imutabilidade/cadeia de hash).

create table correcao_marcacao (
    id             uuid primary key,
    tenant_id      uuid not null references tenant (id),
    vinculo_id     uuid not null references vinculo (id),
    data_hora      timestamptz not null,
    tipo           varchar(20) not null
        check (tipo in ('ENTRADA', 'SAIDA', 'INTERVALO_INICIO', 'INTERVALO_FIM')),
    motivo         varchar(500) not null,
    status         varchar(12) not null
        check (status in ('PENDENTE', 'APROVADA', 'REJEITADA')),
    registro_id    uuid references registro_ponto (id),
    motivo_decisao varchar(500),
    solicitado_em  timestamptz not null default now(),
    decidido_em    timestamptz
);
create index ix_correcao_tenant_status on correcao_marcacao (tenant_id, status);
create index ix_correcao_vinculo on correcao_marcacao (vinculo_id);

alter table correcao_marcacao enable row level security;
alter table correcao_marcacao force row level security;
create policy rls_correcao_marcacao on correcao_marcacao
    using (tenant_id = current_setting('app.current_tenant', true)::uuid)
    with check (tenant_id = current_setting('app.current_tenant', true)::uuid);
