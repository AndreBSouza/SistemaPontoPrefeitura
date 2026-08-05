-- V26: calendario oficial do municipio (12.4.5 / 12.1.6) — feriados, pontos
-- facultativos e abonos coletivos. lotacao_id nulo = vale para todo o ente.
-- A data vira dia nao util na apuracao (nao gera falta).

create table evento_calendario (
    id          uuid primary key,
    tenant_id   uuid not null references tenant (id),
    data        date not null,
    tipo        varchar(20) not null check (tipo in ('FERIADO', 'PONTO_FACULTATIVO', 'ABONO_COLETIVO')),
    descricao   varchar(200) not null,
    lotacao_id  uuid references lotacao (id),
    criado_em   timestamptz not null default now()
);
create index ix_evento_calendario_data on evento_calendario (tenant_id, data);

alter table evento_calendario enable row level security;
alter table evento_calendario force row level security;
create policy rls_evento_calendario on evento_calendario
    using (tenant_id = current_setting('app.current_tenant', true)::uuid)
    with check (tenant_id = current_setting('app.current_tenant', true)::uuid);
