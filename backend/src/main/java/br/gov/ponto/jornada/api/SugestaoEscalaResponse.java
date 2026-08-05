package br.gov.ponto.jornada.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Sugestão de escala (12.4.11) — distribuição diária + dias que ficaram sem cobertura suficiente. */
public record SugestaoEscalaResponse(
        String competencia,
        int coberturaPorDia,
        int maxConsecutivos,
        int servidores,
        List<DiaSugestao> dias,
        List<String> diasDescobertos
) {
    public record DiaSugestao(LocalDate data, List<UUID> vinculos) {
    }
}
