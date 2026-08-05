-- V19: politica de geofence por orgao. null/false = apenas sinaliza a batida fora da
-- cerca (comportamento atual); true = bloqueia a batida fora da area permitida.

alter table lotacao add column geofence_bloqueia boolean;
