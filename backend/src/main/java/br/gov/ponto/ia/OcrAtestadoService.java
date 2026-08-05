package br.gov.ponto.ia;

import br.gov.ponto.funcionalidade.FuncionalidadeService;
import br.gov.ponto.funcionalidade.domain.Funcionalidade;
import br.gov.ponto.ia.api.OcrAtestadoResponse;
import org.springframework.stereotype.Service;

/** OCR de atestado médico, ligável no painel (IA_OCR) e só se houver provedor. */
@Service
public class OcrAtestadoService implements LeitorAtestado {

    private final FuncionalidadeService funcionalidadeService;
    private final IaProvider ia;

    public OcrAtestadoService(FuncionalidadeService funcionalidadeService, IaProvider ia) {
        this.funcionalidadeService = funcionalidadeService;
        this.ia = ia;
    }

    @Override
    public OcrAtestadoResponse ler(byte[] imagem, String contentType) {
        if (!funcionalidadeService.habilitada(Funcionalidade.IA_OCR) || !ia.disponivel()) {
            return OcrAtestadoResponse.indisponivel();
        }
        return new OcrAtestadoResponse(true, ia.lerAtestado(imagem, contentType));
    }
}
