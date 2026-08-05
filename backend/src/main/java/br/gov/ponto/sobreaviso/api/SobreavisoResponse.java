package br.gov.ponto.sobreaviso.api;

import br.gov.ponto.sobreaviso.domain.Sobreaviso;

import java.time.LocalDate;
import java.util.UUID;

public record SobreavisoResponse(
        UUID id,
        UUID vinculoId,
        LocalDate data,
        int minutos,
        String observacao
) {
    public static SobreavisoResponse from(Sobreaviso s) {
        return new SobreavisoResponse(s.getId(), s.getVinculoId(), s.getData(),
                s.getMinutos(), s.getObservacao());
    }
}
