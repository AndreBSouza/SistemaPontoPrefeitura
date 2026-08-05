package br.gov.ponto.cadastro.domain;

/**
 * Cerca geografica circular (centro + raio em metros). Regra de dominio pura e
 * testavel: decide se uma coordenada esta dentro da area permitida do orgao.
 */
public record Geofence(double latitude, double longitude, int raioMetros) {

    private static final double RAIO_TERRA_METROS = 6_371_000d;

    /** Distancia em metros entre o centro da cerca e o ponto informado (Haversine). */
    public double distanciaMetros(double lat, double lng) {
        double dLat = Math.toRadians(lat - latitude);
        double dLng = Math.toRadians(lng - longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(lat))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return RAIO_TERRA_METROS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /** Verdadeiro se o ponto esta dentro (ou na borda) da cerca. */
    public boolean contem(double lat, double lng) {
        return distanciaMetros(lat, lng) <= raioMetros;
    }
}
