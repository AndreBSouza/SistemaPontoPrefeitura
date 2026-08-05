-- V40: hora-atividade do magistério (Lei do Piso, 12.5.8). Minutos semanais da jornada
-- dedicados à hora-atividade (planejamento, fora de sala). Opcional (null = não se aplica).
-- O mínimo legal é 1/3 da carga; a conformidade é verificada em relatório.

alter table jornada add column hora_atividade_min integer;
