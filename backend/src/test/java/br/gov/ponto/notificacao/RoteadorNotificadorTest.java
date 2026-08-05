package br.gov.ponto.notificacao;

import br.gov.ponto.notificacao.domain.CanalNotificacao;
import br.gov.ponto.notificacao.domain.Notificacao;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class RoteadorNotificadorTest {

    private Notificacao nota(String destino, CanalNotificacao canal) {
        return new Notificacao(UUID.randomUUID(), destino, "Assunto", "corpo", canal);
    }

    @Test
    void emailComAdaptadorVaiParaOAdaptador() {
        List<String> recebidos = new ArrayList<>();
        EmailSender email = (dest, ass, msg) -> recebidos.add(dest);
        var r = new RoteadorNotificador(List.of(email), List.of(), new LogNotificador());

        r.enviar(nota("joao@ente.gov.br", CanalNotificacao.EMAIL));

        assertThat(recebidos).containsExactly("joao@ente.gov.br");
    }

    @Test
    void pushSemAdaptadorNaoVazaParaOEmail() {
        List<String> recebidos = new ArrayList<>();
        EmailSender email = (dest, ass, msg) -> recebidos.add(dest);
        var r = new RoteadorNotificador(List.of(email), List.of(), new LogNotificador());

        r.enviar(nota("joao@ente.gov.br", CanalNotificacao.PUSH));

        assertThat(recebidos).isEmpty(); // PUSH sem PushSender cai no log, nunca no e-mail
    }

    @Test
    void falhaNoAdaptadorCaiNoLogSemLancar() {
        EmailSender quebrado = (d, a, m) -> { throw new RuntimeException("smtp fora do ar"); };
        var r = new RoteadorNotificador(List.of(quebrado), List.of(), new LogNotificador());

        assertThatCode(() -> r.enviar(nota("x@x", CanalNotificacao.EMAIL))).doesNotThrowAnyException();
    }
}
