package br.gov.ponto.migracao;

import br.gov.ponto.bancohoras.BancoHorasService;
import br.gov.ponto.cadastro.ServidorService;
import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.cadastro.api.CriarVinculoRequest;
import br.gov.ponto.cadastro.domain.Regime;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.migracao.api.MigracaoSaldoResponse;
import br.gov.ponto.tenant.TenantService;
import br.gov.ponto.tenant.api.CriarTenantRequest;
import br.gov.ponto.tenant.domain.TipoPoder;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class MigracaoServiceTest {

    @Autowired
    private TenantService tenantService;
    @Autowired
    private ServidorService servidorService;
    @Autowired
    private MigracaoService migracaoService;
    @Autowired
    private BancoHorasService bancoHorasService;

    private UUID vinculoId;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        var id = tenantService.criar(new CriarTenantRequest("Ente M", "ente-m", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(id.toString());
        var servidor = servidorService.criar(new CriarServidorRequest("12345678901", "Fulano", null,
                List.of(new CriarVinculoRequest("M-001", Regime.ESTATUTARIO, null, null))));
        vinculoId = servidor.vinculos().get(0).id();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void importaSaldoInicialEEhIdempotente() {
        MigracaoSaldoResponse r = migracaoService.importarSaldoBancoHoras(
                "matricula;saldoMinutos\nM-001;600", LocalDate.of(2026, 6, 1));
        assertThat(r.importados()).isEqualTo(1);
        assertThat(bancoHorasService.saldo(vinculoId)).isEqualTo(600);

        // re-rodar não dobra o saldo (idempotente)
        MigracaoSaldoResponse r2 = migracaoService.importarSaldoBancoHoras(
                "M-001;600", LocalDate.of(2026, 6, 1));
        assertThat(r2.ignorados()).isEqualTo(1);
        assertThat(r2.importados()).isZero();
        assertThat(bancoHorasService.saldo(vinculoId)).isEqualTo(600);
    }

    @Test
    void matriculaInexistenteViraErroDeLinha() {
        MigracaoSaldoResponse r = migracaoService.importarSaldoBancoHoras("NAO-EXISTE;120", LocalDate.of(2026, 6, 1));
        assertThat(r.importados()).isZero();
        assertThat(r.erros()).hasSize(1);
    }

    @Test
    void toleraBomEHeaderAposLinhaEmBranco() {
        // CSV UTF-8 do Excel: BOM + linha em branco + cabeçalho fora da 1ª linha não pode virar erro.
        String csv = "﻿\nmatricula;saldoMinutos\nM-001;120";
        MigracaoSaldoResponse r = migracaoService.importarSaldoBancoHoras(csv, LocalDate.of(2026, 6, 1));
        assertThat(r.total()).isEqualTo(1);
        assertThat(r.importados()).isEqualTo(1);
        assertThat(r.erros()).isEmpty();
        assertThat(bancoHorasService.saldo(vinculoId)).isEqualTo(120);
    }
}
