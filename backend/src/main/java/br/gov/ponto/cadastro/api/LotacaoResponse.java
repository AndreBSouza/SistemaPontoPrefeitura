package br.gov.ponto.cadastro.api;

import br.gov.ponto.cadastro.domain.Lotacao;
import br.gov.ponto.cadastro.domain.RegrasPonto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LotacaoResponse(
        UUID id,
        String nome,
        String sigla,
        UUID chefiaServidorId,
        UUID jornadaPadraoId,
        Integer toleranciaMinutos,
        Boolean bancoHorasHabilitado,
        BigDecimal geofenceLatitude,
        BigDecimal geofenceLongitude,
        Integer geofenceRaioMetros,
        Integer tetoBancoHorasMinutos,
        Boolean verificacaoObrigatoria,
        LocalDate adaptacaoAte,
        Boolean teletrabalho
) {
    public static LotacaoResponse from(Lotacao l) {
        RegrasPonto r = l.getRegras();
        return new LotacaoResponse(l.getId(), l.getNome(), l.getSigla(), l.getChefiaServidorId(),
                r.getJornadaPadraoId(), r.getToleranciaMinutos(), r.getBancoHorasHabilitado(),
                r.getGeofenceLatitude(), r.getGeofenceLongitude(), r.getGeofenceRaioMetros(),
                r.getTetoBancoHorasMinutos(), r.getVerificacaoObrigatoria(),
                r.getAdaptacaoAte(), r.getTeletrabalho());
    }
}
