package br.gov.ponto.saas.api;

import br.gov.ponto.saas.domain.Contrato;
import br.gov.ponto.saas.domain.ModalidadeContratacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ContratoResponse(
        UUID id,
        ModalidadeContratacao modalidade,
        String modalidadeRotulo,
        String numeroProcesso,
        String empenho,
        LocalDate vigenciaInicio,
        LocalDate vigenciaFim,
        BigDecimal valorGlobal,
        BigDecimal valorMensal,
        String observacao,
        boolean vigente
) {
    public static ContratoResponse from(Contrato c, LocalDate hoje) {
        return new ContratoResponse(c.getId(), c.getModalidade(), c.getModalidade().rotulo(),
                c.getNumeroProcesso(), c.getEmpenho(), c.getVigenciaInicio(), c.getVigenciaFim(),
                c.getValorGlobal(), c.getValorMensal(), c.getObservacao(), c.vigenteEm(hoje));
    }
}
