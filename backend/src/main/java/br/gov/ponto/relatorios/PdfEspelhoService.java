package br.gov.ponto.relatorios;

import br.gov.ponto.cadastro.ServidorRepository;
import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.cadastro.domain.Servidor;
import br.gov.ponto.common.tempo.TempoMunicipal;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.tenant.TenantLogoRepository;
import br.gov.ponto.espelho.EspelhoService;
import br.gov.ponto.espelho.api.EspelhoResponse;
import br.gov.ponto.apuracao.api.ApuracaoDiaResponse;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Renderiza o espelho de ponto mensal em PDF (imprimível) a partir da apuração do
 * período. Usa OpenPDF; o conteúdo é o mesmo do espelho exibido no app/painel.
 */
@Service
public class PdfEspelhoService {

    private static final Font TITULO = new Font(Font.HELVETICA, 16, Font.BOLD);
    private static final Font ROTULO = new Font(Font.HELVETICA, 10, Font.BOLD);
    private static final Font NORMAL = new Font(Font.HELVETICA, 10, Font.NORMAL);

    private final EspelhoService espelhoService;
    private final VinculoRepository vinculoRepository;
    private final ServidorRepository servidorRepository;
    private final TenantLogoRepository logoRepository;

    public PdfEspelhoService(EspelhoService espelhoService,
                             VinculoRepository vinculoRepository,
                             ServidorRepository servidorRepository,
                             TenantLogoRepository logoRepository) {
        this.espelhoService = espelhoService;
        this.vinculoRepository = vinculoRepository;
        this.servidorRepository = servidorRepository;
        this.logoRepository = logoRepository;
    }

    @Transactional(readOnly = true)
    public byte[] gerarPdf(UUID vinculoId, YearMonth competencia) {
        UUID tenantId = TenantContext.requireCurrent();
        EspelhoResponse espelho = espelhoService.gerar(vinculoId, competencia);
        String nome = vinculoRepository.findByIdAndTenantId(vinculoId, tenantId)
                .flatMap(v -> servidorRepository.findByIdAndTenantId(v.getServidorId(), tenantId))
                .map(Servidor::getNome)
                .orElse("Servidor");
        return render(espelho, nome);
    }

    /** Declaração de frequência (autoatendimento do servidor) em PDF. */
    @Transactional(readOnly = true)
    public byte[] gerarDeclaracao(UUID vinculoId, YearMonth competencia) {
        UUID tenantId = TenantContext.requireCurrent();
        EspelhoResponse espelho = espelhoService.gerar(vinculoId, competencia);
        Servidor s = vinculoRepository.findByIdAndTenantId(vinculoId, tenantId)
                .flatMap(v -> servidorRepository.findByIdAndTenantId(v.getServidorId(), tenantId))
                .orElse(null);
        String nome = s != null ? s.getNome() : "Servidor";
        String cpf = s != null && s.getCpf() != null ? s.getCpf() : "";
        long diasComRegistro = espelho.dias().stream().filter(d -> d.minutosTrabalhados() > 0).count();

        Document doc = new Document(PageSize.A4, 48, 48, 56, 48);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();
            adicionarLogo(doc);
            doc.add(new Paragraph("DECLARAÇÃO DE FREQUÊNCIA", TITULO));
            doc.add(new Paragraph(" ", NORMAL));
            Paragraph corpo = new Paragraph("Declaramos, para os devidos fins, que " + nome
                    + (cpf.isBlank() ? "" : " (CPF " + cpf + ")")
                    + " registrou frequência na competência " + competencia + ", com "
                    + hhmm(espelho.totalMinutosTrabalhados()) + " trabalhadas em " + diasComRegistro
                    + " dia(s) com registro de ponto.", NORMAL);
            corpo.setSpacingAfter(18);
            doc.add(corpo);
            doc.add(new Paragraph("Emitida em "
                    + LocalDate.now(TempoMunicipal.ZONE).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    + ".", NORMAL));
            doc.add(new Paragraph(" ", NORMAL));
            doc.add(new Paragraph("Documento gerado eletronicamente pelo Ponto Municipal; "
                    + "a autenticidade pode ser conferida junto ao RH do ente.",
                    new Font(Font.HELVETICA, 8, Font.ITALIC)));
            doc.close();
            return out.toByteArray();
        } catch (DocumentException ex) {
            throw new IllegalStateException("Falha ao gerar a declaração", ex);
        }
    }

    private byte[] render(EspelhoResponse e, String nome) {
        Document doc = new Document(PageSize.A4, 36, 36, 48, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();
            adicionarLogo(doc);

            doc.add(new Paragraph("Espelho de Ponto", TITULO));
            doc.add(new Paragraph("Servidor: " + nome, NORMAL));
            doc.add(new Paragraph("Competência: " + e.competencia() + "    Status: " + e.status(), NORMAL));
            doc.add(new Paragraph("Total trabalhado: " + hhmm(e.totalMinutosTrabalhados())
                    + "    Total esperado: " + hhmm(e.totalMinutosEsperados()), NORMAL));
            doc.add(new Paragraph(" ", NORMAL));

            PdfPTable tabela = new PdfPTable(new float[]{2.2f, 2f, 2f, 4f});
            tabela.setWidthPercentage(100);
            cabecalho(tabela, "Data", "Trabalhado", "Esperado", "Ocorrências");
            for (ApuracaoDiaResponse d : e.dias()) {
                tabela.addCell(celula(d.data().toString()));
                tabela.addCell(celula(hhmm(d.minutosTrabalhados())));
                tabela.addCell(celula(hhmm(d.minutosEsperados())));
                String ocs = d.ocorrencias().stream()
                        .map(o -> o.tipo() + (o.minutos() > 0 ? " (" + o.minutos() + "m)" : ""))
                        .collect(Collectors.joining(", "));
                tabela.addCell(celula(ocs.isEmpty() ? "-" : ocs));
            }
            doc.add(tabela);

            doc.add(new Paragraph(" ", NORMAL));
            doc.add(new Paragraph("Documento gerado eletronicamente pelo Ponto Municipal.",
                    new Font(Font.HELVETICA, 8, Font.ITALIC)));
            doc.close();
            return out.toByteArray();
        } catch (DocumentException ex) {
            throw new IllegalStateException("Falha ao gerar o PDF do espelho", ex);
        }
    }

    private void cabecalho(PdfPTable tabela, String... titulos) {
        for (String t : titulos) {
            PdfPCell c = new PdfPCell(new Phrase(t, ROTULO));
            c.setHorizontalAlignment(Element.ALIGN_LEFT);
            c.setGrayFill(0.85f);
            tabela.addCell(c);
        }
    }

    private PdfPCell celula(String texto) {
        return new PdfPCell(new Phrase(texto, NORMAL));
    }

    /** Adiciona o logo do ente (white-label) no topo do documento, se houver. Best-effort. */
    private void adicionarLogo(Document doc) {
        logoRepository.findById(TenantContext.requireCurrent()).ifPresent(logo -> {
            try {
                Image img = Image.getInstance(logo.getConteudo());
                img.scaleToFit(120, 40);
                doc.add(img);
            } catch (Exception e) {
                // logo ausente/ilegível → o documento segue sem ele.
            }
        });
    }

    private static String hhmm(int minutos) {
        return (minutos / 60) + "h " + String.format("%02dmin", minutos % 60);
    }
}
