package br.gov.ponto.me.api;

import java.time.Instant;

/**
 * Um evento da trilha pessoal do servidor (12.1.2): o que aconteceu com seus registros e
 * quem decidiu (correções de marcação e justificativas/atestados). Transparência/anti-objeção.
 */
public record TrilhaEvento(
        Instant instante,
        String categoria,
        String titulo,
        String detalhe
) {
}
