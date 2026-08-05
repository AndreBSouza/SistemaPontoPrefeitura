package br.gov.ponto.ativacao.api;

import br.gov.ponto.ativacao.domain.Dispositivo;

import java.time.Instant;
import java.util.UUID;

public record DispositivoResponse(
        UUID id,
        UUID vinculoId,
        String nome,
        boolean ativo,
        Instant criadoEm
) {
    public static DispositivoResponse from(Dispositivo d) {
        return new DispositivoResponse(d.getId(), d.getVinculoId(), d.getNome(), d.isAtivo(), d.getCriadoEm());
    }
}
