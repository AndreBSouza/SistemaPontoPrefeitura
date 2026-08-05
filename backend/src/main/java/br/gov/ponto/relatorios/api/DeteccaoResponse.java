package br.gov.ponto.relatorios.api;

import java.util.List;
import java.util.UUID;

/**
 * Resultado da varredura de irregularidades (12.5.2): acúmulo ilícito de cargos
 * (jornadas sobrepostas) e "servidores fantasma" (vínculo ativo sem nenhuma batida
 * no período). Insumo de controle interno — sem biometria.
 */
public record DeteccaoResponse(
        List<Acumulo> acumulos,
        List<Fantasma> fantasmas
) {
    /** Dois vínculos ativos do mesmo servidor com jornadas que se sobrepõem. */
    public record Acumulo(UUID servidorId, String servidor, UUID vinculoA, UUID vinculoB) {
    }

    /** Vínculo ativo sem nenhuma batida no período apurado. */
    public record Fantasma(UUID servidorId, String servidor, UUID vinculoId, String matricula) {
    }
}
