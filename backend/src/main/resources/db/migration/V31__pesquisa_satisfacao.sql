-- V31: pesquisa de satisfação/feedback do servidor (12.1.3) — métrica de aceitação.

create table pesquisa_satisfacao (
    id          uuid primary key,
    tenant_id   uuid not null references tenant (id),
    vinculo_id  uuid not null references vinculo (id),
    nota        integer not null check (nota between 1 and 5),
    comentario  varchar(500),
    criado_em   timestamptz not null default now()
);
create index ix_satisfacao_tenant on pesquisa_satisfacao (tenant_id);

alter table pesquisa_satisfacao enable row level security;
alter table pesquisa_satisfacao force row level security;
create policy rls_pesquisa_satisfacao on pesquisa_satisfacao
    using (tenant_id = current_setting('app.current_tenant', true)::uuid)
    with check (tenant_id = current_setting('app.current_tenant', true)::uuid);
