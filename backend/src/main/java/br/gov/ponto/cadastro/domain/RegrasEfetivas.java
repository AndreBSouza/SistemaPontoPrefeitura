package br.gov.ponto.cadastro.domain;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Regras de ponto efetivas de um vinculo, ja resolvida a heranca:
 * override do vinculo (escala vigente) → regras do orgao → defaults do sistema.
 */
public record RegrasEfetivas(
        UUID jornadaEfetivaId,
        boolean comIntervalo,
        Integer toleranciaMinutos,
        boolean bancoHorasHabilitado,
        BigDecimal geofenceLatitude,
        BigDecimal geofenceLongitude,
        Integer geofenceRaioMetros,
        boolean teletrabalho
) {
    public boolean temGeofence() {
        return geofenceLatitude != null && geofenceLongitude != null && geofenceRaioMetros != null;
    }

    /**
     * Cerca geografica do orgao, se configurada. Em teletrabalho/home office a cerca nao se
     * aplica (o servidor bate de onde estiver) — retorna vazio.
     */
    public Optional<Geofence> geofence() {
        if (teletrabalho || !temGeofence()) {
            return Optional.empty();
        }
        return Optional.of(new Geofence(geofenceLatitude.doubleValue(), geofenceLongitude.doubleValue(), geofenceRaioMetros));
    }
}
