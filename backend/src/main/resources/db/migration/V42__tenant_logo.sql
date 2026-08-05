-- V42: logo do ente armazenada no banco (12.2.1, alternativa ao S3 sem infra externa).
-- Um logo por tenant (PK = tenant_id). Servido por endpoint público por slug.

create table tenant_logo (
    tenant_id     uuid primary key references tenant (id),
    content_type  varchar(100) not null,
    conteudo      bytea not null,
    atualizado_em timestamptz not null default now()
);

alter table tenant_logo enable row level security;
alter table tenant_logo force row level security;
create policy rls_tenant_logo on tenant_logo
    using (tenant_id = current_setting('app.current_tenant', true)::uuid)
    with check (tenant_id = current_setting('app.current_tenant', true)::uuid);
