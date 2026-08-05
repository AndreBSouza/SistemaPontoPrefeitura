package br.gov.ponto.migracao.api;

import java.util.List;

/** Resultado do import de saldo inicial de banco de horas (migração zero-dor). */
public record MigracaoSaldoResponse(int total, int importados, int ignorados, List<String> erros) {
}
