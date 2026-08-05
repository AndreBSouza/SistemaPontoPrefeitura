package br.gov.ponto.ia;

import br.gov.ponto.common.error.ConsentimentoNecessarioException;
import br.gov.ponto.ia.api.OcrAtestadoResponse;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AtestadoOcrServiceTest {

    private final UUID vinculo = UUID.randomUUID();

    @Test
    void semConsentimentoNaoChamaOcr() {
        boolean[] chamou = {false};
        LeitorAtestado ocr = (img, ct) -> {
            chamou[0] = true;
            return new OcrAtestadoResponse(true, null);
        };
        AtestadoOcrService svc = new AtestadoOcrService(ocr, (v, f) -> false);

        assertThatThrownBy(() -> svc.ler(vinculo, new byte[]{1}, "image/png"))
                .isInstanceOf(ConsentimentoNecessarioException.class);
        assertThat(chamou[0]).isFalse(); // não pode enviar dado de saúde ao provedor sem consentimento
    }

    @Test
    void comConsentimentoNaFinalidadeCertaDelega() {
        OcrAtestadoResponse esperado = new OcrAtestadoResponse(true, null);
        LeitorAtestado ocr = (img, ct) -> esperado;
        // só concede para a finalidade IA_OCR_SAUDE — garante que o serviço pede a finalidade certa
        AtestadoOcrService svc = new AtestadoOcrService(ocr, (v, f) -> f.equals(AtestadoOcrService.FINALIDADE));

        assertThat(svc.ler(vinculo, new byte[]{1}, "image/png")).isSameAs(esperado);
    }
}
