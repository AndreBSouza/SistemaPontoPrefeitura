package br.gov.ponto.delegacao.api;

import br.gov.ponto.delegacao.domain.Delegacao;

import java.time.LocalDate;
import java.util.UUID;

public record DelegacaoResponse(
        UUID id,
        UUID deleganteServidorId,
        UUID delegadoServidorId,
        LocalDate dataInicio,
        LocalDate dataFim,
        boolean ativo
) {
    public static DelegacaoResponse from(Delegacao d) {
        return new DelegacaoResponse(d.getId(), d.getDeleganteServidorId(), d.getDelegadoServidorId(),
                d.getDataInicio(), d.getDataFim(), d.isAtivo());
    }
}
