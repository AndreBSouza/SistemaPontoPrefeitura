package br.gov.ponto.ia;

import br.gov.ponto.common.error.ConsentimentoNecessarioException;
import br.gov.ponto.ia.api.OcrAtestadoResponse;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * OCR de atestado do servidor COM gate de consentimento LGPD. O atestado carrega dado de saúde
 * (CID = categoria especial, LGPD art. 11) e o OCR o envia a um provedor externo — por isso exige
 * consentimento específico do titular ({@link #FINALIDADE}) antes de processar. O servidor concede
 * via {@code POST /api/me/lgpd/consentimento} (finalidade {@code IA_OCR_SAUDE}).
 */
@Service
public class AtestadoOcrService {

    /** Finalidade de consentimento para o OCR de atestado (dado de saúde via IA externa). */
    public static final String FINALIDADE = "IA_OCR_SAUDE";

    private final LeitorAtestado ocrAtestadoService;
    private final ConsentimentoServidor consentimento;

    public AtestadoOcrService(LeitorAtestado ocrAtestadoService, ConsentimentoServidor consentimento) {
        this.ocrAtestadoService = ocrAtestadoService;
        this.consentimento = consentimento;
    }

    public OcrAtestadoResponse ler(UUID vinculoId, byte[] imagem, String contentType) {
        if (!consentimento.consentimento(vinculoId, FINALIDADE)) {
            throw new ConsentimentoNecessarioException(FINALIDADE,
                    "É necessário consentir com o processamento do atestado por IA (dado de saúde) antes de usar o OCR.");
        }
        return ocrAtestadoService.ler(imagem, contentType);
    }
}
