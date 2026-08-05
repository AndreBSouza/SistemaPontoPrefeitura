package br.gov.ponto.me;

import br.gov.ponto.cadastro.ServidorService;
import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.cadastro.api.CriarVinculoRequest;
import br.gov.ponto.cadastro.domain.Regime;
import br.gov.ponto.common.error.AcessoNegadoException;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.me.api.ComprovanteRepResponse;
import br.gov.ponto.registro.RegistroService;
import br.gov.ponto.registro.api.BaterPontoRequest;
import br.gov.ponto.relatorios.AfdService;
import br.gov.ponto.tenant.BrandingService;
import br.gov.ponto.tenant.TenantService;
import br.gov.ponto.tenant.api.CriarTenantRequest;
import br.gov.ponto.tenant.domain.TipoPoder;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprovante de Registro de Ponto do Trabalhador (art. 79 da Portaria MTP 671/2021).
 *
 * <p>O ponto central: o código hash do comprovante tem de ser <b>o mesmo</b> que sai no AFD. Se
 * divergissem, o auditor-fiscal teria razão em recusar o arquivo — e o trabalhador não teria como
 * provar que sua marcação é a que está no arquivo.</p>
 */
@SpringBootTest(properties = {
        "rep.inpi=51202300012345",
        "rep.desenvolvedor.cnpj=99888777000166"
})
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class ComprovanteRepTest {

    @Autowired
    private TenantService tenantService;
    @Autowired
    private BrandingService brandingService;
    @Autowired
    private ServidorService servidorService;
    @Autowired
    private RegistroService registroService;
    @Autowired
    private ComprovanteRepService comprovanteRepService;
    @Autowired
    private ComprovantePdfService comprovantePdfService;
    @Autowired
    private AfdService afdService;

    private UUID vinculoId;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        UUID tenantId = tenantService.criar(
                new CriarTenantRequest("Prefeitura Teste", "ente-comprovante", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenantId.toString());
        brandingService.definirCnpj("11222333000181");

        var servidor = servidorService.criar(new CriarServidorRequest(
                "12345678901", "Joana da Silva", "joana@ente.gov.br",
                List.of(new CriarVinculoRequest("M-1", Regime.ESTATUTARIO, "Analista", 40))));
        vinculoId = servidor.vinculos().get(0).id();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private long bater() {
        return registroService.bater(new BaterPontoRequest(
                vinculoId, br.gov.ponto.registro.domain.OrigemRegistro.MOBILE, null,
                null, null, false, UUID.randomUUID().toString())).nsr();
    }

    @Test
    void comprovanteTemOConteudoMinimoDoArt79() {
        long nsr = bater();

        ComprovanteRepResponse c = comprovanteRepService.porNsr(vinculoId, nsr);

        assertThat(c.titulo()).isEqualTo("Comprovante de Registro de Ponto do Trabalhador"); // I
        assertThat(c.nsr()).isEqualTo(nsr);                                                  // II
        assertThat(c.empregadorNome()).isEqualTo("Prefeitura Teste");                        // III
        assertThat(c.empregadorCnpj()).isEqualTo("11222333000181");                          // III
        assertThat(c.localPrestacaoServico()).isNotBlank();                                  // IV
        assertThat(c.trabalhadorNome()).isEqualTo("Joana da Silva");                         // V
        assertThat(c.trabalhadorCpf()).isEqualTo("12345678901");                             // V
        assertThat(c.dataHoraRegistro()).matches("\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}");       // VI
        assertThat(c.registroInpi()).isEqualTo("51202300012345");                            // VII
        assertThat(c.codigoHash()).matches("[0-9a-f]{64}");                                  // VIII
    }

    @Test
    void hashDoComprovanteEOMesmoQueSaiNoAfd() {
        long nsr = bater();

        String doComprovante = comprovanteRepService.porNsr(vinculoId, nsr).codigoHash();

        String afd = afdService.gerar(YearMonth.now()).conteudo();
        String registroTipo7 = afd.lines()
                .filter(l -> l.length() == 137 && l.charAt(9) == '7')
                .findFirst().orElseThrow();
        String hashNoAfd = registroTipo7.substring(73, 137);

        assertThat(doComprovante)
                .as("o hash mostrado ao trabalhador precisa ser o mesmo entregue à fiscalização")
                .isEqualTo(hashNoAfd);
    }

    @Test
    void hashEncadeiaEntreMarcacoesSucessivas() {
        long primeira = bater();
        long segunda = bater();

        String h1 = comprovanteRepService.porNsr(vinculoId, primeira).codigoHash();
        String h2 = comprovanteRepService.porNsr(vinculoId, segunda).codigoHash();

        assertThat(h1).isNotEqualTo(h2);
    }

    @Test
    void comprovanteEmPdfTrazOHashPorExtenso() throws Exception {
        long nsr = bater();
        String hash = comprovanteRepService.porNsr(vinculoId, nsr).codigoHash();

        byte[] pdf = comprovantePdfService.gerar(vinculoId, nsr);

        assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        // O hash precisa estar legível no documento: é com ele que o servidor confere a marcação
        // contra o AFD. O texto é extraído do PDF para garantir que não ficou só nos metadados.
        var leitor = new com.lowagie.text.pdf.PdfReader(pdf);
        String texto = new com.lowagie.text.pdf.parser.PdfTextExtractor(leitor).getTextFromPage(1);
        leitor.close();
        assertThat(texto.replaceAll("\\s", "")).contains(hash);
        assertThat(texto).contains("Comprovante de Registro de Ponto do Trabalhador");
    }

    @Test
    void semCertificadoOPdfAvisaQueNaoEstaAssinado() throws Exception {
        // Neste ambiente de teste nao ha 'assinatura.keystore', entao prevalece o assinador no-op.
        // O comprovante ainda e' entregue, mas dizendo a verdade sobre o proprio valor probatorio.
        long nsr = bater();
        byte[] pdf = comprovantePdfService.gerar(vinculoId, nsr);

        var leitor = new com.lowagie.text.pdf.PdfReader(pdf);
        String texto = new com.lowagie.text.pdf.parser.PdfTextExtractor(leitor).getTextFromPage(1);
        assertThat(leitor.getAcroFields().getSignatureNames()).isEmpty();
        leitor.close();
        assertThat(texto).contains("NAO assinado digitalmente");
    }

    @Test
    void servidorNaoExtraiComprovanteDeMarcacaoDeOutroVinculo() {
        long nsr = bater();
        var outro = servidorService.criar(new CriarServidorRequest(
                "98765432100", "Outro Servidor", null,
                List.of(new CriarVinculoRequest("M-2", Regime.ESTATUTARIO, "Auxiliar", 40))));
        UUID outroVinculo = outro.vinculos().get(0).id();

        assertThatThrownBy(() -> comprovanteRepService.porNsr(outroVinculo, nsr))
                .isInstanceOf(AcessoNegadoException.class);
    }
}
