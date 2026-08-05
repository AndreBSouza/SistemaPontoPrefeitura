package br.gov.ponto.ausencia.api;

import br.gov.ponto.ausencia.domain.TipoAusencia;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Cobertura da equipe de um órgão na competência (12.4.1): total de vínculos e quem está
 * ausente (férias/licença) e quando — para o RH não deixar o setor descoberto.
 */
public record CoberturaResponse(
        UUID lotacaoId,
        String competencia,
        int totalVinculos,
        List<Item> ausencias
) {
    public record Item(UUID vinculoId, String servidor, TipoAusencia tipo,
                       LocalDate dataInicio, LocalDate dataFim) {
    }
}
