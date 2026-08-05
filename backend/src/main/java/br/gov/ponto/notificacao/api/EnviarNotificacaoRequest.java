package br.gov.ponto.notificacao.api;

import br.gov.ponto.notificacao.domain.CanalNotificacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EnviarNotificacaoRequest(
        @NotBlank String destinatario,
        @NotBlank String assunto,
        String mensagem,
        @NotNull CanalNotificacao canal
) {
}
