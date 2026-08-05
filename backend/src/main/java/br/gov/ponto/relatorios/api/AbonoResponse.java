package br.gov.ponto.relatorios.api;

import br.gov.ponto.apuracao.domain.StatusJustificativa;
import br.gov.ponto.apuracao.domain.TipoJustificativa;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Linha do relatório de abonos e exceções (12.6.15): justificativa com tipo, período,
 * status e a decisão (motivo + quando + quem aprovou, quando o login estiver mapeado).
 */
public record AbonoResponse(
        UUID id,
        UUID vinculoId,
        String servidor,
        TipoJustificativa tipo,
        LocalDate dataInicio,
        LocalDate dataFim,
        StatusJustificativa status,
        String motivoDecisao,
        Instant decididoEm,
        UUID aprovadorId
) {
}
