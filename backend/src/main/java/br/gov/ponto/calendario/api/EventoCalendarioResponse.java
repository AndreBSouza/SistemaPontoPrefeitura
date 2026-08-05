package br.gov.ponto.calendario.api;

import br.gov.ponto.calendario.domain.EventoCalendario;
import br.gov.ponto.calendario.domain.TipoEventoCalendario;

import java.time.LocalDate;
import java.util.UUID;

public record EventoCalendarioResponse(
        UUID id,
        LocalDate data,
        TipoEventoCalendario tipo,
        String descricao,
        UUID lotacaoId,
        boolean geral
) {
    public static EventoCalendarioResponse from(EventoCalendario e) {
        return new EventoCalendarioResponse(e.getId(), e.getData(), e.getTipo(),
                e.getDescricao(), e.getLotacaoId(), e.isGeral());
    }
}
