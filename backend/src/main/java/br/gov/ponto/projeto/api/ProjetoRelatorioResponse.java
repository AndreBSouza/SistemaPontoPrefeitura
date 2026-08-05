package br.gov.ponto.projeto.api;

import java.util.List;
import java.util.UUID;

/** Relatório de apropriação por projeto/convênio na competência (total de horas). */
public record ProjetoRelatorioResponse(
        String competencia,
        List<Linha> projetos
) {
    public record Linha(UUID projetoId, String nome, String fonte, int totalMinutos, int lancamentos) {
    }
}
