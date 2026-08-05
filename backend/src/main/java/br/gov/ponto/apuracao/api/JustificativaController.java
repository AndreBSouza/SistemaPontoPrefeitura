package br.gov.ponto.apuracao.api;

import br.gov.ponto.apuracao.JustificativaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/justificativas")
public class JustificativaController {

    private final JustificativaService justificativaService;

    public JustificativaController(JustificativaService justificativaService) {
        this.justificativaService = justificativaService;
    }

    @PostMapping
    public ResponseEntity<JustificativaResponse> solicitar(
            @Valid @RequestBody SolicitarJustificativaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(justificativaService.solicitar(request));
    }

    @GetMapping
    public List<JustificativaResponse> listarPorVinculo(@RequestParam UUID vinculoId) {
        return justificativaService.listarPorVinculo(vinculoId);
    }

    @GetMapping("/pendentes")
    public List<JustificativaResponse> listarPendentes() {
        return justificativaService.listarPendentes();
    }

    @GetMapping("/chefia/{chefiaServidorId}/pendentes")
    public List<JustificativaResponse> pendentesDaChefia(@PathVariable UUID chefiaServidorId) {
        return justificativaService.pendentesDaChefia(chefiaServidorId);
    }

    @PostMapping("/{id}/aprovar")
    public JustificativaResponse aprovar(@PathVariable UUID id,
                                         @RequestBody(required = false) DecisaoRequest decisao) {
        return justificativaService.aprovarComAlcada(id,
                decisao == null ? null : decisao.motivoDecisao(), aprovadorEhRh());
    }

    /**
     * Alçada (12.6.13): RH/controladoria/admin aprovam sem limite; um gestor fica sujeito ao teto
     * de dias. Modo aberto/dev (sem perfis) não aplica alçada.
     */
    private boolean aprovadorEhRh() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities().isEmpty()) {
            return true;
        }
        Set<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        boolean ehRh = roles.contains("ROLE_rh") || roles.contains("ROLE_controladoria")
                || roles.contains("ROLE_tenant-admin");
        boolean ehGestor = roles.contains("ROLE_gestor");
        // Só limita quem é gestor e não acumula perfil de RH/controle.
        return ehRh || !ehGestor;
    }

    @PostMapping("/{id}/rejeitar")
    public JustificativaResponse rejeitar(@PathVariable UUID id,
                                          @RequestBody(required = false) DecisaoRequest decisao) {
        return justificativaService.rejeitar(id, decisao == null ? null : decisao.motivoDecisao());
    }

    /** Aprovação em lote (caixa de aprovações do RH/chefia). */
    @PostMapping("/lote/aprovar")
    public List<JustificativaResponse> aprovarEmLote(@Valid @RequestBody DecisaoLoteRequest request) {
        return justificativaService.aprovarEmLote(request.ids(), request.motivoDecisao());
    }

    /** Recusa em lote (caixa de aprovações do RH/chefia). */
    @PostMapping("/lote/rejeitar")
    public List<JustificativaResponse> rejeitarEmLote(@Valid @RequestBody DecisaoLoteRequest request) {
        return justificativaService.rejeitarEmLote(request.ids(), request.motivoDecisao());
    }
}
