-- V11: registro de notificacoes enviadas (push/e-mail)

create table notificacao (
    id           uuid primary key,
    tenant_id    uuid not null references tenant (id),
    destinatario varchar(200) not null,
    assunto      varchar(200) not null,
    mensagem     varchar(1000),
    canal        varchar(10) not null check (canal in ('PUSH', 'EMAIL')),
    enviada_em   timestamptz not null default now()
);
create index ix_notificacao_tenant on notificacao (tenant_id);
create index ix_notificacao_dest on notificacao (tenant_id, destinatario);

alter table notificacao enable row level security;
alter table notificacao force row level security;
create policy rls_notificacao on notificacao
    using (tenant_id = current_setting('app.current_tenant', true)::uuid)
    with check (tenant_id = current_setting('app.current_tenant', true)::uuid);
