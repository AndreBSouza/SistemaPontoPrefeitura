-- V44: feature flags por ente — ativar/desativar funcionalidades pelo painel admin.
-- Cobre anomalias heurísticas (12.3.12) e recursos de IA plugáveis (12.4.8/9/10).
-- Opt-in: quando não há linha, vale o default do sistema (desligado).

create table funcionalidade_flag (
    id            uuid primary key,
    tenant_id     uuid not null references tenant (id),
    chave         varchar(40) not null,
    habilitado    boolean not null,
    atualizado_em timestamptz not null default now(),
    unique (tenant_id, chave)
);
create index ix_funcionalidade_flag_tenant on funcionalidade_flag (tenant_id);

alter table funcionalidade_flag enable row level security;
alter table funcionalidade_flag force row level security;
create policy rls_funcionalidade_flag on funcionalidade_flag
    using (tenant_id = current_setting('app.current_tenant', true)::uuid)
    with check (tenant_id = current_setting('app.current_tenant', true)::uuid);
