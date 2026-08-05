-- V25: comunicados oficiais (broadcast) da prefeitura aos servidores (12.3.7).
-- lotacao_id nulo = comunicado geral (todos os orgaos).

create table comunicado (
    id           uuid primary key,
    tenant_id    uuid not null references tenant (id),
    titulo       varchar(200) not null,
    mensagem     varchar(4000) not null,
    lotacao_id   uuid references lotacao (id),
    publicado_em timestamptz not null default now()
);
create index ix_comunicado_tenant on comunicado (tenant_id, publicado_em desc);

alter table comunicado enable row level security;
alter table comunicado force row level security;
create policy rls_comunicado on comunicado
    using (tenant_id = current_setting('app.current_tenant', true)::uuid)
    with check (tenant_id = current_setting('app.current_tenant', true)::uuid);
