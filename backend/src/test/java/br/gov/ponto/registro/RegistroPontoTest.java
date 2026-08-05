package br.gov.ponto.registro;

import br.gov.ponto.cadastro.ServidorService;
import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.cadastro.api.CriarVinculoRequest;
import br.gov.ponto.cadastro.domain.Regime;
import br.gov.ponto.common.tempo.TempoMunicipal;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.relatorios.IntegridadeService;
import br.gov.ponto.registro.api.BaterPontoRequest;
import br.gov.ponto.registro.api.RegistrarPontoRequest;
import br.gov.ponto.registro.domain.OrigemRegistro;
import br.gov.ponto.registro.domain.TipoMarcacao;
import br.gov.ponto.tenant.TenantService;
import br.gov.ponto.tenant.api.CriarTenantRequest;
import br.gov.ponto.tenant.domain.TipoPoder;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class RegistroPontoTest {

    @Autowired
    private TenantService tenantService;
    @Autowired
    private ServidorService servidorService;
    @Autowired
    private RegistroService registroService;
    @Autowired
    private RegistroPontoRepository registroRepository;
    @Autowired
    private IntegridadeService integridadeService;

    private UUID vinculoId;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        UUID tenant = tenantService.criar(
                new CriarTenantRequest("Ente Y", "ente-y", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenant.toString());
        var servidor = servidorService.criar(new CriarServidorRequest(
                "55555555555", "Edu", null,
                List.of(new CriarVinculoRequest("M-9", Regime.CELETISTA, "Operario", 44))));
        vinculoId = servidor.vinculos().get(0).id();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void nsrSequencialPorTenant() {
        var c1 = registroService.registrar(new RegistrarPontoRequest(
                vinculoId, TipoMarcacao.ENTRADA, OrigemRegistro.MOBILE, null, null, null, false, "k1"));
        var c2 = registroService.registrar(new RegistrarPontoRequest(
                vinculoId, TipoMarcacao.SAIDA, OrigemRegistro.MOBILE, null, null, null, false, "k2"));

        assertThat(c1.nsr()).isEqualTo(1L);
        assertThat(c2.nsr()).isEqualTo(2L);
    }

    @Test
    void idempotenciaNaoDuplicaRegistro() {
        var a = registroService.registrar(new RegistrarPontoRequest(
                vinculoId, TipoMarcacao.ENTRADA, OrigemRegistro.MOBILE, null, null, null, true, "dup"));
        var b = registroService.registrar(new RegistrarPontoRequest(
                vinculoId, TipoMarcacao.ENTRADA, OrigemRegistro.MOBILE, null, null, null, true, "dup"));

        assertThat(b.id()).isEqualTo(a.id());
        assertThat(b.nsr()).isEqualTo(a.nsr());
        assertThat(registroService.listarPorVinculo(vinculoId)).hasSize(1);
    }

    @Test
    void baterDeduzSequenciaDoDia() {
        var b1 = registroService.bater(bater("b1"));
        var b2 = registroService.bater(bater("b2"));
        var b3 = registroService.bater(bater("b3"));
        var b4 = registroService.bater(bater("b4"));
        var b5 = registroService.bater(bater("b5"));

        // Sem jornada cadastrada para o vinculo, assume-se intervalo (ciclo de 4 fases).
        assertThat(b1.tipo()).isEqualTo(TipoMarcacao.ENTRADA);
        assertThat(b2.tipo()).isEqualTo(TipoMarcacao.INTERVALO_INICIO);
        assertThat(b3.tipo()).isEqualTo(TipoMarcacao.INTERVALO_FIM);
        assertThat(b4.tipo()).isEqualTo(TipoMarcacao.SAIDA);
        assertThat(b5.tipo()).isEqualTo(TipoMarcacao.ENTRADA);
        assertThat(b1.mensagem()).startsWith("Entrada registrada às ");
        assertThat(b1.nsr()).isEqualTo(1L);
        assertThat(b4.nsr()).isEqualTo(4L);
    }

    @Test
    void baterEhIdempotente() {
        var a = registroService.bater(bater("dup-bater"));
        var b = registroService.bater(bater("dup-bater"));

        assertThat(b.id()).isEqualTo(a.id());
        assertThat(b.nsr()).isEqualTo(a.nsr());
        assertThat(registroService.listarPorVinculo(vinculoId)).hasSize(1);
    }

    @Test
    void registrosFormamCadeiaDeHash() {
        registroService.bater(bater("hash-1"));
        registroService.bater(bater("hash-2"));

        var regs = registroRepository.findByVinculoIdAndTenantIdOrderByNsr(vinculoId, TenantContext.requireCurrent());
        assertThat(regs).hasSize(2);
        assertThat(regs.get(0).getHash()).isNotBlank();
        assertThat(regs.get(1).getHashAnterior()).isEqualTo(regs.get(0).getHash());
        assertThat(regs.get(1).getHash()).isNotEqualTo(regs.get(0).getHash());
    }

    @Test
    void cadeiaDeIntegridadeEhVerificavel() {
        registroService.bater(bater("integ-1"));
        registroService.bater(bater("integ-2"));

        var ok = integridadeService.verificar(java.time.YearMonth.now(TempoMunicipal.ZONE));
        assertThat(ok.integra()).isTrue();
        assertThat(ok.totalRegistros()).isEqualTo(2);

        // Adultera o hash de um registro → o verificador acusa o NSR rompido.
        var regs = registroRepository.findByVinculoIdAndTenantIdOrderByNsr(vinculoId, TenantContext.requireCurrent());
        var alvo = regs.get(0);
        alvo.definirCadeia(alvo.getHashAnterior(), "0000000000adulterado");
        registroRepository.saveAndFlush(alvo);

        var rompido = integridadeService.verificar(java.time.YearMonth.now(TempoMunicipal.ZONE));
        assertThat(rompido.integra()).isFalse();
        assertThat(rompido.nsrRompido()).isNotNull();
    }

    private BaterPontoRequest bater(String idempotencyKey) {
        return new BaterPontoRequest(vinculoId, OrigemRegistro.MOBILE, null, null, null, false, idempotencyKey);
    }
}
