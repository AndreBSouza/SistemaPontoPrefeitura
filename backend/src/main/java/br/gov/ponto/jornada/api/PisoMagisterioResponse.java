package br.gov.ponto.jornada.api;

import java.util.UUID;

/**
 * Conformidade da hora-atividade do magistério (Lei do Piso, 12.5.8): pelo menos 1/3 da carga
 * deve ser hora-atividade (fora de sala). {@code atendePiso} indica se a jornada cumpre o mínimo.
 */
public record PisoMagisterioResponse(
        UUID jornadaId,
        String nome,
        int cargaHorariaSemanalMin,
        int horaAtividadeMin,
        int minimoLegalMin,
        double percentual,
        boolean atendePiso
) {
}
