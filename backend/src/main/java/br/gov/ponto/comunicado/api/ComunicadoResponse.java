package br.gov.ponto.comunicado.api;

import br.gov.ponto.comunicado.domain.Comunicado;

import java.time.Instant;
import java.util.UUID;

public record ComunicadoResponse(
        UUID id,
        String titulo,
        String mensagem,
        UUID lotacaoId,
        boolean geral,
        Instant publicadoEm
) {
    public static ComunicadoResponse from(Comunicado c) {
        return new ComunicadoResponse(c.getId(), c.getTitulo(), c.getMensagem(),
                c.getLotacaoId(), c.isGeral(), c.getPublicadoEm());
    }
}
