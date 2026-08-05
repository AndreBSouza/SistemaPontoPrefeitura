package br.gov.ponto.relatorios;

import br.gov.ponto.relatorios.api.RelatorioFrequenciaResponse;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Cache em memória (TTL curto) da apuração de frequência por <b>(ente, vínculo, competência)</b>.
 * Os relatórios pesados (BI executivo, anomalias, dossiê) iteram a frequência de TODOS os vínculos
 * chamando {@code frequenciaMensal}, que reapura o espelho — custoso em entes grandes. Cachear evita
 * reapurar quando o gestor navega entre esses relatórios / atualiza na mesma janela.
 *
 * <p>A CHAVE inclui o tenant — <b>nunca</b> servir a apuração de um ente para outro. O TTL limita a
 * janela de staleness (batidas muito recentes podem não refletir; aceitável em relatório). Em
 * produção multi-instância, trocar por Redis — a interface {@code obter} permanece.</p>
 */
@Component
public class CacheFrequencia {

    private static final long TTL_MS = 60_000;
    private static final int TETO_ENTRADAS = 20_000;

    private record Entrada(RelatorioFrequenciaResponse valor, long expiraEm) {
    }

    private final Map<String, Entrada> mapa = new ConcurrentHashMap<>();

    /** Retorna do cache (se válido) ou calcula via {@code carregar} e memoiza. */
    public RelatorioFrequenciaResponse obter(String chave, Supplier<RelatorioFrequenciaResponse> carregar) {
        long agora = System.currentTimeMillis();
        Entrada e = mapa.get(chave);
        if (e != null && e.expiraEm() > agora) {
            return e.valor();
        }
        RelatorioFrequenciaResponse valor = carregar.get();
        if (mapa.size() > TETO_ENTRADAS) {
            mapa.entrySet().removeIf(en -> en.getValue().expiraEm() <= agora); // evita crescimento indefinido
        }
        mapa.put(chave, new Entrada(valor, agora + TTL_MS));
        return valor;
    }
}
