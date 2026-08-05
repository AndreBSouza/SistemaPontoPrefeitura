package br.gov.ponto.apuracao.domain;

import br.gov.ponto.registro.domain.TipoMarcacao;

/** Marcacao normalizada para apuracao: minuto do dia (0-1439) + tipo. */
public record Marcacao(int minutoDoDia, TipoMarcacao tipo) {
}
