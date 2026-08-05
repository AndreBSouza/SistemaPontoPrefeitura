package br.gov.ponto.relatorios.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Inconsistências de jornada (12.4.15): dias com número ímpar de marcações (intervalo
 * aberto = possível esquecimento de batida). Insumo para o RH/servidor corrigir.
 */
public record InconsistenciasResponse(
        String competencia,
        List<Item> inconsistencias
) {
    public record Item(UUID vinculoId, String servidor, LocalDate data, int marcacoes, String motivo) {
    }
}
