package br.gov.ponto.registro.domain;

import java.util.List;

/**
 * Deduz o tipo da proxima batida a partir da sequencia de batidas ja registradas
 * no dia e da existencia (ou nao) de intervalo na jornada. Regra de dominio pura,
 * sem I/O, testavel isoladamente — base do "botao unico" de ponto.
 *
 * <p>Ciclo com intervalo: ENTRADA → INTERVALO_INICIO → INTERVALO_FIM → SAIDA → (repete).
 * <br>Ciclo sem intervalo: ENTRADA → SAIDA → (repete).
 *
 * <p>A deducao usa a contagem de batidas do dia (modulo o tamanho do ciclo), o que
 * a torna deterministica e robusta a turnos repetidos no mesmo dia.
 */
public final class DeducaoBatida {

    private static final List<TipoMarcacao> CICLO_COM_INTERVALO = List.of(
            TipoMarcacao.ENTRADA,
            TipoMarcacao.INTERVALO_INICIO,
            TipoMarcacao.INTERVALO_FIM,
            TipoMarcacao.SAIDA);

    private static final List<TipoMarcacao> CICLO_SEM_INTERVALO = List.of(
            TipoMarcacao.ENTRADA,
            TipoMarcacao.SAIDA);

    private DeducaoBatida() {
    }

    /**
     * @param batidasDoDia quantidade de batidas ja registradas hoje para o vinculo
     * @param comIntervalo se a jornada vigente no dia preve intervalo (ex.: almoco)
     * @return o tipo deduzido para a proxima batida
     */
    public static TipoMarcacao proximoTipo(int batidasDoDia, boolean comIntervalo) {
        if (batidasDoDia < 0) {
            throw new IllegalArgumentException("batidasDoDia nao pode ser negativo");
        }
        List<TipoMarcacao> ciclo = comIntervalo ? CICLO_COM_INTERVALO : CICLO_SEM_INTERVALO;
        return ciclo.get(batidasDoDia % ciclo.size());
    }
}
