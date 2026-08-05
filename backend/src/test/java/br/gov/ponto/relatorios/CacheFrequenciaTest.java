package br.gov.ponto.relatorios;

import br.gov.ponto.relatorios.api.RelatorioFrequenciaResponse;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class CacheFrequenciaTest {

    private RelatorioFrequenciaResponse dummy() {
        return new RelatorioFrequenciaResponse(UUID.randomUUID(), "2026-08", 0, 0, 0, 0, 0, 0);
    }

    @Test
    void mesmaChaveCalculaUmaVezSo() {
        CacheFrequencia cache = new CacheFrequencia();
        AtomicInteger calls = new AtomicInteger();
        var r1 = cache.obter("t|v|2026-08", () -> { calls.incrementAndGet(); return dummy(); });
        var r2 = cache.obter("t|v|2026-08", () -> { calls.incrementAndGet(); return dummy(); });

        assertThat(calls.get()).isEqualTo(1); // 2º acesso veio do cache
        assertThat(r2).isSameAs(r1);
    }

    @Test
    void chavePorTenantIsola() {
        CacheFrequencia cache = new CacheFrequencia();
        AtomicInteger calls = new AtomicInteger();
        cache.obter("tenantA|v|2026-08", () -> { calls.incrementAndGet(); return dummy(); });
        cache.obter("tenantB|v|2026-08", () -> { calls.incrementAndGet(); return dummy(); });

        assertThat(calls.get()).isEqualTo(2); // não serve a apuração de um ente para outro
    }
}
