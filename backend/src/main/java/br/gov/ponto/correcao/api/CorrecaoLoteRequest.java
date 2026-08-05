package br.gov.ponto.correcao.api;

import br.gov.ponto.registro.domain.TipoMarcacao;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Correção de marcação em lote pelo RH (12.6.4): várias marcações de um vínculo de uma vez. */
public record CorrecaoLoteRequest(
        @NotNull UUID vinculoId,
        @NotEmpty List<Item> itens,
        @Size(max = 500) String motivo
) {
    public record Item(@NotNull Instant dataHora, @NotNull TipoMarcacao tipo) {
    }
}
