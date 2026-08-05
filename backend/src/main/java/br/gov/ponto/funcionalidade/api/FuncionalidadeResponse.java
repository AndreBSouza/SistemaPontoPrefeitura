package br.gov.ponto.funcionalidade.api;

/** Uma funcionalidade e seu estado (ligada/desligada) para o painel. */
public record FuncionalidadeResponse(String chave, String rotulo, boolean habilitado) {
}
