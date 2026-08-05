package br.gov.ponto.correcao.api;

import br.gov.ponto.apuracao.api.DecisaoRequest;
import br.gov.ponto.correcao.CorrecaoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Correção de marcação do lado administrativo (chefia/RH): caixa de pendentes,
 * aprovação/recusa e correção direta em lote. A criação de marcação respeita a
 * imutabilidade (nova batida encadeada, origem AJUSTE).
 */
@RestController
@RequestMapping("/api/correcoes")
public class CorrecaoController {

    private final CorrecaoService correcaoService;

    public CorrecaoController(CorrecaoService correcaoService) {
        this.correcaoService = correcaoService;
    }

    @GetMapping("/pendentes")
    public List<CorrecaoResponse> pendentes() {
        return correcaoService.listarPendentes().stream().map(CorrecaoResponse::from).toList();
    }

    @PostMapping("/{id}/aprovar")
    public CorrecaoResponse aprovar(@PathVariable UUID id,
                                    @RequestBody(required = false) DecisaoRequest decisao) {
        return CorrecaoResponse.from(
                correcaoService.aprovar(id, decisao == null ? null : decisao.motivoDecisao()));
    }

    @PostMapping("/{id}/rejeitar")
    public CorrecaoResponse rejeitar(@PathVariable UUID id,
                                     @RequestBody(required = false) DecisaoRequest decisao) {
        return CorrecaoResponse.from(
                correcaoService.rejeitar(id, decisao == null ? null : decisao.motivoDecisao()));
    }

    /** Correção direta do RH, em lote (12.6.4). */
    @PostMapping("/lote")
    public List<CorrecaoResponse> corrigirEmLote(@Valid @RequestBody CorrecaoLoteRequest request) {
        return correcaoService.corrigirEmLote(request.vinculoId(), request.itens(), request.motivo())
                .stream().map(CorrecaoResponse::from).toList();
    }
}
