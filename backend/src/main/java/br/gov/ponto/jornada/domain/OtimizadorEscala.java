package br.gov.ponto.jornada.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiPredicate;

/**
 * Heurística (sem IA) de sugestão de escala: distribui a cobertura diária entre os servidores
 * disponíveis, priorizando quem trabalhou menos (justiça) e respeitando um teto de dias
 * consecutivos (descanso). É uma sugestão — quem decide é o gestor.
 */
public final class OtimizadorEscala {

    private OtimizadorEscala() {
    }

    public record Sugestao(Map<LocalDate, List<UUID>> escala, List<LocalDate> diasDescobertos) {
    }

    /**
     * @param disponivel (servidor, data) -&gt; pode trabalhar naquele dia (ex.: não está de férias)
     * @param coberturaPorDia quantidade desejada de servidores por dia
     * @param maxConsecutivos teto de dias seguidos de trabalho antes de forçar folga
     */
    public static Sugestao montar(List<UUID> servidores, List<LocalDate> dias,
                                  BiPredicate<UUID, LocalDate> disponivel,
                                  int coberturaPorDia, int maxConsecutivos) {
        Map<UUID, Integer> total = new HashMap<>();
        Map<UUID, Integer> streak = new HashMap<>();
        for (UUID s : servidores) {
            total.put(s, 0);
            streak.put(s, 0);
        }
        Map<LocalDate, List<UUID>> escala = new LinkedHashMap<>();
        List<LocalDate> descobertos = new ArrayList<>();

        for (LocalDate dia : dias) {
            List<UUID> candidatos = new ArrayList<>();
            for (UUID s : servidores) {
                if (disponivel.test(s, dia) && streak.get(s) < maxConsecutivos) {
                    candidatos.add(s);
                }
            }
            // justiça: menos dias trabalhados primeiro; desempate: menor sequência (mais descansado)
            candidatos.sort(Comparator
                    .comparingInt((UUID s) -> total.get(s))
                    .thenComparingInt(streak::get));
            List<UUID> escolhidos = new ArrayList<>(
                    candidatos.subList(0, Math.min(coberturaPorDia, candidatos.size())));
            escala.put(dia, escolhidos);
            if (escolhidos.size() < coberturaPorDia) {
                descobertos.add(dia);
            }

            Set<UUID> hoje = new HashSet<>(escolhidos);
            for (UUID s : servidores) {
                if (hoje.contains(s)) {
                    total.merge(s, 1, Integer::sum);
                    streak.merge(s, 1, Integer::sum);
                } else {
                    streak.put(s, 0);
                }
            }
        }
        return new Sugestao(escala, descobertos);
    }
}
