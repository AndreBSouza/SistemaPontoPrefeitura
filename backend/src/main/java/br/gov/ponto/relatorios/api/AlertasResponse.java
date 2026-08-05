package br.gov.ponto.relatorios.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Alertas de risco proativos para o controle interno (12.6.10): ajustes manuais de
 * banco de horas (vetor de abuso) e batidas marcadas fora da cerca no período.
 * Hora extra atípica fica disponível por vínculo no relatório de frequência; vínculos
 * sem batida ("fantasma") ficam no relatório de detecção (/api/relatorios/acumulo).
 */
public record AlertasResponse(
        List<AjusteManual> ajustesManuais,
        List<ForaDaCerca> batidasForaDaCerca
) {
    /** Lançamento manual no banco de horas (tipo AJUSTE) — quem/quanto/quando. */
    public record AjusteManual(UUID vinculoId, String servidor, LocalDate data, int minutos, String descricao) {
    }

    /** Vínculo com batidas marcadas fora da cerca no período. */
    public record ForaDaCerca(UUID vinculoId, String servidor, long quantidade) {
    }
}
