package br.gov.ponto.cadastro.api;

import br.gov.ponto.cadastro.domain.Regime;
import br.gov.ponto.cadastro.domain.Vinculo;

import java.util.UUID;

public record VinculoResponse(
        UUID id,
        String matricula,
        Regime regime,
        String cargo,
        Integer cargaHorariaSemanal,
        UUID lotacaoId
) {
    public static VinculoResponse from(Vinculo v) {
        return new VinculoResponse(v.getId(), v.getMatricula(), v.getRegime(),
                v.getCargo(), v.getCargaHorariaSemanal(), v.getLotacaoId());
    }
}
