package br.gov.ponto.calendario.api;

import br.gov.ponto.calendario.domain.TipoEventoCalendario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/** Criação de evento do calendário. {@code lotacaoId} nulo = vale para todo o ente. */
public record CriarEventoRequest(
        @NotNull LocalDate data,
        @NotNull TipoEventoCalendario tipo,
        @NotBlank @Size(max = 200) String descricao,
        UUID lotacaoId
) {
}
