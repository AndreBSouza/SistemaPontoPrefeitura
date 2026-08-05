package br.gov.ponto.correcao.api;

import br.gov.ponto.correcao.domain.CorrecaoMarcacao;
import br.gov.ponto.correcao.domain.StatusCorrecao;
import br.gov.ponto.registro.domain.TipoMarcacao;

import java.time.Instant;
import java.util.UUID;

public record CorrecaoResponse(
        UUID id,
        UUID vinculoId,
        Instant dataHora,
        TipoMarcacao tipo,
        String motivo,
        StatusCorrecao status,
        UUID registroId,
        String motivoDecisao,
        Instant solicitadoEm,
        Instant decididoEm
) {
    public static CorrecaoResponse from(CorrecaoMarcacao c) {
        return new CorrecaoResponse(c.getId(), c.getVinculoId(), c.getDataHora(), c.getTipo(),
                c.getMotivo(), c.getStatus(), c.getRegistroId(), c.getMotivoDecisao(),
                c.getSolicitadoEm(), c.getDecididoEm());
    }
}
