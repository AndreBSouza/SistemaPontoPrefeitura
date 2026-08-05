-- V35: teletrabalho/home office por órgão (12.4.2) — quando true, a geofence não se aplica
-- (o servidor bate de qualquer lugar). Opcional (null = não é teletrabalho).

alter table lotacao add column teletrabalho boolean;
