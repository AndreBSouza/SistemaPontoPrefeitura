package br.gov.ponto.cadastro.api;

import br.gov.ponto.cadastro.domain.Regime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CriarVinculoRequest(
        @NotBlank String matricula,
        @NotNull Regime regime,
        String cargo,
        Integer cargaHorariaSemanal,
        UUID lotacaoId
) {
    /** Compat: cadastro sem lotação (mantém chamadas antigas e o seeder/testes funcionando). */
    public CriarVinculoRequest(String matricula, Regime regime, String cargo, Integer cargaHorariaSemanal) {
        this(matricula, regime, cargo, cargaHorariaSemanal, null);
    }
}
