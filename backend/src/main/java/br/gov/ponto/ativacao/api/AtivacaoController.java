package br.gov.ponto.ativacao.api;

import br.gov.ponto.ativacao.AtivacaoService;
import br.gov.ponto.ativacao.LimiteAtivacao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ativacao")
public class AtivacaoController {

    private final AtivacaoService ativacaoService;
    private final LimiteAtivacao limiteAtivacao;

    public AtivacaoController(AtivacaoService ativacaoService, LimiteAtivacao limiteAtivacao) {
        this.ativacaoService = ativacaoService;
        this.limiteAtivacao = limiteAtivacao;
    }

    /** RH: gera o codigo de ativacao para um vinculo. */
    @PostMapping("/codigos")
    public ResponseEntity<GerarCodigoResponse> gerar(@Valid @RequestBody GerarCodigoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ativacaoService.gerar(request.vinculoId(), request.validadeHoras()));
    }

    /** App (sem login): identidade visual do ente resolvida pelo código, para a tela de ativação. */
    @GetMapping("/branding")
    public br.gov.ponto.tenant.api.BrandingResponse branding(@RequestParam String codigo) {
        return ativacaoService.brandingDoCodigo(codigo);
    }

    /** App (sem login): troca o codigo por um token de dispositivo. */
    @PostMapping("/ativar")
    public AtivacaoResponse ativar(@Valid @RequestBody AtivarRequest request, HttpServletRequest http) {
        if (!limiteAtivacao.permitir(http.getRemoteAddr())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Muitas tentativas de ativação. Tente novamente em instantes.");
        }
        return ativacaoService.ativar(request.codigo(), request.nomeDispositivo());
    }

    /** RH: lista os dispositivos de um vinculo. */
    @GetMapping("/dispositivos")
    public List<DispositivoResponse> listar(@RequestParam UUID vinculoId) {
        return ativacaoService.listarDispositivos(vinculoId).stream().map(DispositivoResponse::from).toList();
    }

    /** RH: revoga (desativa) um dispositivo. */
    @PostMapping("/dispositivos/{id}/revogar")
    public ResponseEntity<Void> revogar(@PathVariable UUID id) {
        ativacaoService.revogar(id);
        return ResponseEntity.noContent().build();
    }
}
