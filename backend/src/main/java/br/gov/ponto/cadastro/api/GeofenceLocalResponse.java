package br.gov.ponto.cadastro.api;

import br.gov.ponto.cadastro.domain.GeofenceLocal;

import java.math.BigDecimal;
import java.util.UUID;

public record GeofenceLocalResponse(
        UUID id,
        UUID lotacaoId,
        String nome,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer raioMetros
) {
    public static GeofenceLocalResponse from(GeofenceLocal g) {
        return new GeofenceLocalResponse(g.getId(), g.getLotacaoId(), g.getNome(),
                g.getLatitude(), g.getLongitude(), g.getRaioMetros());
    }
}
