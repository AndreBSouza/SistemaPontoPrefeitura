-- V47: contrato de fornecimento ao ente (valor FIXO — dispensa/licitação, não cobrança por demanda).
-- Substitui o antigo "billing por servidor ativo", que não cabe na contratação do setor público.

create table contrato (
    id              uuid primary key,
    tenant_id       uuid not null references tenant (id),
    modalidade      varchar(20) not null,
    numero_processo varchar(60),
    empenho         varchar(60),
    vigencia_inicio date not null,
    vigencia_fim    date not null,
    valor_global    numeric(15, 2),
    valor_mensal    numeric(15, 2),
    observacao      varchar(500),
    criado_em       timestamptz not null default now()
);
create index ix_contrato_tenant on contrato (tenant_id);

alter table contrato enable row level security;
alter table contrato force row level security;
create policy rls_contrato on contrato
    using (tenant_id = current_setting('app.current_tenant', true)::uuid)
    with check (tenant_id = current_setting('app.current_tenant', true)::uuid);
