-- V38: áreas de referência adicionais por órgão (multi-geofence / locais volantes, 12.3.10).
-- Um órgão pode ter várias áreas; a batida é "fora da área" só quando fora de todas (cálculo
-- combina com a cerca primária do órgão). Apenas verificação do admin — nunca bloqueia/alerta.

create table geofence_local (
    id          uuid primary key,
    tenant_id   uuid not null references tenant (id),
    lotacao_id  uuid not null references lotacao (id),
    nome        varchar(120) not null,
    latitude    numeric(9, 6) not null,
    longitude   numeric(9, 6) not null,
    raio_metros integer not null,
    criado_em   timestamptz not null default now()
);
create index ix_geofence_local_lotacao on geofence_local (tenant_id, lotacao_id);

alter table geofence_local enable row level security;
alter table geofence_local force row level security;
create policy rls_geofence_local on geofence_local
    using (tenant_id = current_setting('app.current_tenant', true)::uuid)
    with check (tenant_id = current_setting('app.current_tenant', true)::uuid);
