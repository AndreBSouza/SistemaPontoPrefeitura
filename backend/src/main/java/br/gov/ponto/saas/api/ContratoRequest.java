package br.gov.ponto.saas.api;

import br.gov.ponto.saas.domain.ModalidadeContratacao;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContratoRequest(
        @NotNull ModalidadeContratacao modalidade,
        String numeroProcesso,
        String empenho,
        @NotNull LocalDate vigenciaInicio,
        @NotNull LocalDate vigenciaFim,
        BigDecimal valorGlobal,
        BigDecimal valorMensal,
        String observacao
) {
}
