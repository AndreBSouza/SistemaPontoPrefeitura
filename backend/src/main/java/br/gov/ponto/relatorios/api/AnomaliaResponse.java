package br.gov.ponto.relatorios.api;

import java.util.List;
import java.util.UUID;

/** Resultado da detecção de anomalias. {@code habilitada=false} = funcionalidade desligada no ente. */
public record AnomaliaResponse(boolean habilitada, List<Anomalia> anomalias) {

    public record Anomalia(String tipo, UUID vinculoId, String servidor, String detalhe) {
    }
}
