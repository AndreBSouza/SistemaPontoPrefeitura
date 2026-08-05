package br.gov.ponto.correcao.api;

import br.gov.ponto.registro.domain.TipoMarcacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * Solicitação de correção de marcação. {@code vinculoId} é obrigatório quando criada pelo
 * RH/chefia; no app do servidor o vínculo vem do dispositivo (campo ignorado).
 */
public record SolicitarCorrecaoRequest(
        UUID vinculoId,
        @NotNull Instant dataHora,
        @NotNull TipoMarcacao tipo,
        @NotBlank @Size(max = 500) String motivo
) {
}
