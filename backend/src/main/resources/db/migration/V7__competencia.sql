-- V7: competencia (fechamento mensal + ciencia/assinatura do servidor)
-- ano_mes = primeiro dia do mes de competencia.

create table competencia (
    id                uuid primary key,
    tenant_id         uuid not null references tenant (id),
    vinculo_id        uuid not null references vinculo (id),
    ano_mes           date not null,
    status            varchar(10) not null check (status in ('ABERTA', 'FECHADA')),
    fechado_em        timestamptz,
    reaberto_em       timestamptz,
    motivo_reabertura varchar(500),
    ciencia_em        timestamptz,
    ciencia_evidencia varchar(200),
    criado_em         timestamptz not null default now(),
    constraint uq_competencia unique (vinculo_id, ano_mes)
);
create index ix_competencia_tenant on competencia (tenant_id);

alter table competencia enable row level security;
alter table competencia force row level security;
create policy rls_competencia on competencia
    using (tenant_id = current_setting('app.current_tenant', true)::uuid)
    with check (tenant_id = current_setting('app.current_tenant', true)::uuid);
