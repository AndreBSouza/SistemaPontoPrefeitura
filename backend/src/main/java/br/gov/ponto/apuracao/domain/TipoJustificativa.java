package br.gov.ponto.apuracao.domain;

import java.util.Set;

/** Tipo de abono/justificativa e as ocorrencias que ele neutraliza na apuracao. */
public enum TipoJustificativa {
    FALTA(Set.of(TipoOcorrencia.FALTA)),
    ATRASO(Set.of(TipoOcorrencia.ATRASO)),
    SAIDA_ANTECIPADA(Set.of(TipoOcorrencia.SAIDA_ANTECIPADA)),
    LICENCA(Set.of(TipoOcorrencia.FALTA)),
    FERIAS(Set.of(TipoOcorrencia.FALTA)),
    ATESTADO(Set.of(TipoOcorrencia.FALTA)),
    OUTRO(Set.of());

    private final Set<TipoOcorrencia> neutralizadas;

    TipoJustificativa(Set<TipoOcorrencia> neutralizadas) {
        this.neutralizadas = neutralizadas;
    }

    public boolean neutraliza(TipoOcorrencia ocorrencia) {
        return neutralizadas.contains(ocorrencia);
    }
}
