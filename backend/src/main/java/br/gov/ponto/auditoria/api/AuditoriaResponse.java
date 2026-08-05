package br.gov.ponto.auditoria.api;

import br.gov.ponto.auditoria.domain.AuditoriaEvento;

import java.time.Instant;
import java.util.UUID;

public record AuditoriaResponse(
        UUID id,
        String acao,
        String entidade,
        String entidadeId,
        String ator,
        String detalhe,
        Instant ocorridoEm
) {
    public static AuditoriaResponse from(AuditoriaEvento e) {
        return new AuditoriaResponse(e.getId(), e.getAcao(), e.getEntidade(),
                e.getEntidadeId(), e.getAtor(), e.getDetalhe(), e.getOcorridoEm());
    }
}
