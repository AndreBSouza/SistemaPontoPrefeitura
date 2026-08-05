package br.gov.ponto.projeto.api;

import br.gov.ponto.projeto.ProjetoService;
import br.gov.ponto.projeto.domain.Projeto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/** Projetos/convênios e apropriação de horas (prestação de contas). */
@RestController
@RequestMapping("/api/projetos")
public class ProjetoController {

    private final ProjetoService projetoService;

    public ProjetoController(ProjetoService projetoService) {
        this.projetoService = projetoService;
    }

    public record CriarProjetoRequest(@NotBlank @Size(max = 200) String nome, @Size(max = 120) String fonte) {
    }

    public record ApropriarRequest(@NotNull UUID vinculoId, @NotNull UUID projetoId,
                                   @NotNull LocalDate data, @Positive int minutos,
                                   @Size(max = 300) String descricao) {
    }

    public record ProjetoResponse(UUID id, String nome, String fonte) {
        static ProjetoResponse from(Projeto p) {
            return new ProjetoResponse(p.getId(), p.getNome(), p.getFonte());
        }
    }

    @PostMapping
    public ResponseEntity<ProjetoResponse> criar(@Valid @RequestBody CriarProjetoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ProjetoResponse.from(projetoService.criar(req.nome(), req.fonte())));
    }

    @GetMapping
    public List<ProjetoResponse> listar() {
        return projetoService.listar().stream().map(ProjetoResponse::from).toList();
    }

    @PostMapping("/apropriacoes")
    public ResponseEntity<Void> apropriar(@Valid @RequestBody ApropriarRequest req) {
        projetoService.apropriar(req.vinculoId(), req.projetoId(), req.data(), req.minutos(), req.descricao());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/relatorio")
    public ProjetoRelatorioResponse relatorio(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth competencia) {
        return projetoService.relatorio(competencia);
    }
}
