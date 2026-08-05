package br.gov.ponto.registro.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CadeiaHashTest {

    private final UUID tenant = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID vinculo = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private final Instant momento = Instant.parse("2026-06-24T11:00:00Z");

    @Test
    void deterministicoParaOsMesmosDados() {
        String a = CadeiaHash.calcular("", tenant, vinculo, 1, TipoMarcacao.ENTRADA, momento, "k1");
        String b = CadeiaHash.calcular("", tenant, vinculo, 1, TipoMarcacao.ENTRADA, momento, "k1");
        assertThat(a).isEqualTo(b).hasSize(64);
    }

    @Test
    void hashAnteriorDiferenteMudaOHash() {
        String g = CadeiaHash.calcular("", tenant, vinculo, 1, TipoMarcacao.ENTRADA, momento, "k1");
        String h2a = CadeiaHash.calcular(g, tenant, vinculo, 2, TipoMarcacao.SAIDA, momento, "k2");
        String h2b = CadeiaHash.calcular("outro-anterior", tenant, vinculo, 2, TipoMarcacao.SAIDA, momento, "k2");
        assertThat(h2a).isNotEqualTo(h2b);
    }

    @Test
    void alterarCampoMudaOHash() {
        String base = CadeiaHash.calcular("", tenant, vinculo, 1, TipoMarcacao.ENTRADA, momento, "k1");
        String mexido = CadeiaHash.calcular("", tenant, vinculo, 1, TipoMarcacao.SAIDA, momento, "k1");
        assertThat(base).isNotEqualTo(mexido);
    }
}
