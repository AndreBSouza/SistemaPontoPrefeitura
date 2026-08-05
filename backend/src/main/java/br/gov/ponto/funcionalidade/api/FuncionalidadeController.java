package br.gov.ponto.funcionalidade.api;

import br.gov.ponto.funcionalidade.FuncionalidadeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Painel admin: liga/desliga funcionalidades do ente (anomalias, IA…). */
@RestController
@RequestMapping("/api/funcionalidades")
public class FuncionalidadeController {

    private final FuncionalidadeService service;

    public FuncionalidadeController(FuncionalidadeService service) {
        this.service = service;
    }

    @GetMapping
    public List<FuncionalidadeResponse> listar() {
        return service.listar();
    }

    @PutMapping("/{chave}")
    public void definir(@PathVariable String chave, @RequestParam boolean habilitado) {
        service.definir(chave, habilitado);
    }
}
