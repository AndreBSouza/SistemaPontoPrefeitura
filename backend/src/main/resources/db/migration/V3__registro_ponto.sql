-- V3: registro de ponto + sequencia de NSR por tenant

-- Contador de NSR por tenant (sequencial e imutavel). Sem RLS: acesso por tenant_id explicito.
create table nsr_sequencia (
    tenant_id   uuid primary key references tenant (id),
    proximo_nsr bigint not null
);

create table registro_ponto (
    id                    uuid primary key,
    tenant_id             uuid not null references tenant (id),
    vinculo_id            uuid not null references vinculo (id),
    nsr                   bigint not null,
    tipo                  varchar(20) not null
        check (tipo in ('ENTRADA', 'SAIDA', 'INTERVALO_INICIO', 'INTERVALO_FIM')),
    origem                varchar(20) not null check (origem in ('MOBILE', 'WEB', 'TOTEM')),
    data_hora_servidor    timestamptz not null,
    data_hora_dispositivo timestamptz,
    latitude              numeric(9, 6),
    longitude             numeric(9, 6),
    offline               boolean not null default false,
    idempotency_key       varchar(80) not null,
    criado_em             timestamptz not null default now(),
    constraint uq_registro_tenant_nsr unique (tenant_id, nsr),
    constraint uq_registro_tenant_idem unique (tenant_id, idempotency_key)
);
create index ix_registro_tenant on registro_ponto (tenant_id);
create index ix_registro_vinculo on registro_ponto (vinculo_id);

alter table registro_ponto enable row level security;
alter table registro_ponto force row level security;
create policy rls_registro on registro_ponto
    using (tenant_id = current_setting('app.current_tenant', true)::uuid)
    with check (tenant_id = current_setting('app.current_tenant', true)::uuid);
