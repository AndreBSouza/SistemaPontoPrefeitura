-- V37: a geofence passou a ser apenas verificação para o administrador — nunca bloqueia
-- nem alerta o servidor. A política de bloqueio (geofence_bloqueia) não existe mais.

alter table lotacao drop column if exists geofence_bloqueia;
