package br.gov.ponto.apuracao.api;

import br.gov.ponto.apuracao.domain.Justificativa;
import br.gov.ponto.apuracao.domain.StatusJustificativa;
import br.gov.ponto.apuracao.domain.TipoJustificativa;

import java.time.LocalDate;
import java.util.UUID;

public record JustificativaResponse(
        UUID id,
        UUID vinculoId,
        TipoJustificativa tipo,
        LocalDate dataInicio,
        LocalDate dataFim,
        String motivo,
        String anexo,
        StatusJustificativa status,
        String motivoDecisao
) {
    public static JustificativaResponse from(Justificativa j) {
        return new JustificativaResponse(j.getId(), j.getVinculoId(), j.getTipo(),
                j.getDataInicio(), j.getDataFim(), j.getMotivo(), j.getAnexo(),
                j.getStatus(), j.getMotivoDecisao());
    }
}
