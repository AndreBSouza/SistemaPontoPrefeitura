package br.gov.ponto.jornada.api;

import br.gov.ponto.jornada.domain.Jornada;
import br.gov.ponto.jornada.domain.TipoJornada;

import java.util.UUID;

public record JornadaResponse(
        UUID id,
        String nome,
        TipoJornada tipo,
        int cargaHorariaSemanalMin,
        int toleranciaMin,
        int intervaloMin,
        Integer horaAtividadeMin,
        boolean ativo
) {
    public static JornadaResponse from(Jornada j) {
        return new JornadaResponse(j.getId(), j.getNome(), j.getTipo(),
                j.getCargaHorariaSemanalMin(), j.getToleranciaMin(), j.getIntervaloMin(),
                j.getHoraAtividadeMin(), j.isAtivo());
    }
}
