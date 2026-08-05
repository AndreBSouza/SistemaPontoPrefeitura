package br.gov.ponto.me;

import br.gov.ponto.me.api.ComprovanteRepResponse;
import br.gov.ponto.relatorios.AssinadorPdf;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

/**
 * Comprovante de Registro de Ponto do Trabalhador em <b>PDF assinado</b> (art. 80, I, da Portaria
 * MTP 671/2021), com o conteúdo mínimo do art. 79.
 *
 * <p>O código hash aparece por extenso e em destaque: é com ele que o trabalhador confere que a
 * sua marcação é a mesma que consta no AFD entregue à fiscalização.</p>
 */
@Service
public class ComprovantePdfService {

    private static final Font TITULO = new Font(Font.HELVETICA, 14, Font.BOLD);
    private static final Font ROTULO = new Font(Font.HELVETICA, 9, Font.BOLD);
    private static final Font NORMAL = new Font(Font.HELVETICA, 9, Font.NORMAL);
    private static final Font MONO = new Font(Font.COURIER, 8, Font.NORMAL);
    private static final Font AVISO = new Font(Font.HELVETICA, 8, Font.ITALIC);

    private final ComprovanteRepService comprovanteRepService;
    private final AssinadorPdf assinadorPdf;

    public ComprovantePdfService(ComprovanteRepService comprovanteRepService, AssinadorPdf assinadorPdf) {
        this.comprovanteRepService = comprovanteRepService;
        this.assinadorPdf = assinadorPdf;
    }

    /** Gera e assina o comprovante de uma marcação do próprio servidor. */
    public byte[] gerar(UUID vinculoId, long nsr) {
        return assinadorPdf.assinar(montar(comprovanteRepService.porNsr(vinculoId, nsr)));
    }

    private byte[] montar(ComprovanteRepResponse c) {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A5, 32, 32, 32, 32);
        try {
            PdfWriter.getInstance(doc, saida);
            doc.open();

            Paragraph titulo = new Paragraph(c.titulo(), TITULO); // inciso I
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(14);
            doc.add(titulo);

            PdfPTable tabela = new PdfPTable(2);
            tabela.setWidthPercentage(100);
            tabela.setWidths(new float[]{34, 66});

            linha(tabela, "NSR", String.valueOf(c.nsr()));                       // II
            linha(tabela, "Empregador", c.empregadorNome());                     // III
            linha(tabela, "CNPJ/CPF", c.empregadorCnpj());                       // III
            if (c.empregadorCnoCaepf() != null && !c.empregadorCnoCaepf().isBlank()) {
                linha(tabela, "CEI/CAEPF/CNO", c.empregadorCnoCaepf());          // III
            }
            linha(tabela, "Local de trabalho", c.localPrestacaoServico());       // IV
            linha(tabela, "Trabalhador", c.trabalhadorNome());                   // V
            linha(tabela, "CPF", c.trabalhadorCpf());                            // V
            linha(tabela, "Data e hora", c.dataHoraRegistro());                  // VI
            linha(tabela, "Registro no INPI", c.registroInpi());                 // VII
            doc.add(tabela);

            // Inciso VIII — em destaque e por extenso, para poder ser conferido contra o AFD.
            Paragraph rotuloHash = new Paragraph("Código hash da marcação (SHA-256)", ROTULO);
            rotuloHash.setSpacingBefore(12);
            doc.add(rotuloHash);
            Paragraph hash = new Paragraph(quebrar(c.codigoHash()), MONO);
            hash.setSpacingAfter(10);
            doc.add(hash);

            // Inciso IX — a assinatura eletrônica fica embutida no arquivo; o texto abaixo diz ao
            // trabalhador o que esperar, inclusive quando o ente ainda não configurou o certificado.
            doc.add(new Paragraph(assinadorPdf.disponivel()
                    ? "Documento assinado eletronicamente com certificado ICP-Brasil. "
                      + "Verifique a assinatura no painel do seu leitor de PDF."
                    : "ATENCAO: documento ainda NAO assinado digitalmente "
                      + "(certificado ICP-Brasil nao configurado neste ambiente).", AVISO));
            doc.add(new Paragraph(
                    "Este comprovante espelha a marcacao registrada no REP-P. O codigo hash acima e "
                    + "o mesmo que consta no Arquivo-Fonte de Dados (AFD) fornecido a fiscalizacao.",
                    AVISO));

            doc.close();
            return saida.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gerar o comprovante em PDF", e);
        }
    }

    private static void linha(PdfPTable tabela, String rotulo, String valor) {
        tabela.addCell(celula(new Phrase(rotulo, ROTULO)));
        tabela.addCell(celula(new Phrase(valor == null ? "-" : valor, NORMAL)));
    }

    private static PdfPCell celula(Phrase conteudo) {
        PdfPCell celula = new PdfPCell(conteudo);
        celula.setPadding(4);
        return celula;
    }

    /** Quebra o hash em blocos para caber na largura da página sem virar uma linha ilegível. */
    private static String quebrar(String hash) {
        if (hash == null || hash.isBlank()) {
            return "-";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hash.length(); i += 32) {
            sb.append(hash, i, Math.min(i + 32, hash.length()));
            if (i + 32 < hash.length()) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }
}
