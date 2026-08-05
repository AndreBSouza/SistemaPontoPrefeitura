package br.gov.ponto.relatorios.api;

import java.util.List;
import java.util.UUID;

/**
 * Adesão ao ponto eletrônico (12.1.9 painel de adesão por órgão / piloto; 12.1.8 isonomia
 * por regime). "Aderiu" = o vínculo tem ao menos um dispositivo ativo (app/web). Percentual
 * em pontos inteiros.
 */
public record AdesaoResponse(
        List<Grupo> grupos
) {
    /** Recorte de adesão por órgão ou por regime. */
    public record Grupo(String chave, String rotulo, int vinculos, int aderiram, int percentual) {
    }
}
