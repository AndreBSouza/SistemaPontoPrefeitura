-- V4: horarios esperados por dia da semana de cada jornada (base da apuracao)
-- dia_semana segue ISO-8601: 1=segunda ... 7=domingo (java.time.DayOfWeek.getValue()).

create table jornada_horario (
    id           uuid primary key,
    tenant_id    uuid not null references tenant (id),
    jornada_id   uuid not null references jornada (id),
    dia_semana   integer not null check (dia_semana between 1 and 7),
    hora_entrada time not null,
    hora_saida   time not null,
    constraint uq_jornada_horario unique (jornada_id, dia_semana)
);
create index ix_jornada_horario_tenant on jornada_horario (tenant_id);

alter table jornada_horario enable row level security;
alter table jornada_horario force row level security;
create policy rls_jornada_horario on jornada_horario
    using (tenant_id = current_setting('app.current_tenant', true)::uuid)
    with check (tenant_id = current_setting('app.current_tenant', true)::uuid);
