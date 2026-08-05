package br.gov.ponto.notificacao.api;

import br.gov.ponto.notificacao.LembretePendenciaService;
import br.gov.ponto.notificacao.NotificacaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notificacoes")
public class NotificacaoController {

    private final NotificacaoService notificacaoService;
    private final LembretePendenciaService lembretePendenciaService;

    public NotificacaoController(NotificacaoService notificacaoService,
                                 LembretePendenciaService lembretePendenciaService) {
        this.notificacaoService = notificacaoService;
        this.lembretePendenciaService = lembretePendenciaService;
    }

    @PostMapping
    public ResponseEntity<NotificacaoResponse> enviar(@Valid @RequestBody EnviarNotificacaoRequest request) {
        var n = notificacaoService.enviar(request.destinatario(), request.assunto(),
                request.mensagem(), request.canal());
        return ResponseEntity.status(HttpStatus.CREATED).body(NotificacaoResponse.from(n));
    }

    @GetMapping
    public List<NotificacaoResponse> listar(@RequestParam String destinatario) {
        return notificacaoService.listar(destinatario).stream().map(NotificacaoResponse::from).toList();
    }

    /** Dispara lembretes de pendência (ciência do espelho) aos servidores com competência fechada. */
    @PostMapping("/lembretes")
    public Map<String, Integer> lembretes() {
        return Map.of("enviados", lembretePendenciaService.lembrarCienciasPendentes());
    }
}
