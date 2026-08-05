package br.gov.ponto.relatorios.api;

import java.util.List;
import java.util.UUID;

/**
 * BI executivo do prefeito (12.3.2): presença/absenteísmo por secretaria (órgão), com
 * faltas, custo de hora extra (em minutos) e vínculos sem batida ("fantasma"). O ranking
 * é montado na UI sobre estas linhas.
 */
public record BiExecutivoResponse(
        String competencia,
        int totalVinculos,
        int totalFaltas,
        int totalHoraExtraMinutos,
        int totalFantasmas,
        List<OrgaoBi> orgaos
) {
    public record OrgaoBi(UUID lotacaoId, String orgao, int vinculos, int faltas,
                          int horaExtraMinutos, int fantasmas) {
    }
}
