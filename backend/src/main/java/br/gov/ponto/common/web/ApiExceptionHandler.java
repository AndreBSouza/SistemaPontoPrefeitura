package br.gov.ponto.common.web;

import br.gov.ponto.common.error.AcessoNegadoException;
import br.gov.ponto.common.error.ConflitoException;
import br.gov.ponto.common.error.ConsentimentoNecessarioException;
import br.gov.ponto.common.error.RecursoNaoEncontradoException;
import br.gov.ponto.ia.IaFalhaException;
import br.gov.ponto.ia.IaIndisponivelException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<Map<String, Object>> conflito(ConflitoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("erro", ex.getMessage()));
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> naoEncontrado(RecursoNaoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", ex.getMessage()));
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<Map<String, Object>> acessoNegado(AcessoNegadoException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("erro", ex.getMessage()));
    }

    @ExceptionHandler(ConsentimentoNecessarioException.class)
    public ResponseEntity<Map<String, Object>> consentimentoNecessario(ConsentimentoNecessarioException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "erro", ex.getMessage(),
                "codigo", "CONSENTIMENTO_NECESSARIO",
                "finalidade", ex.getFinalidade()));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, Object>> requisicaoInvalida(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("erro", ex.getMessage()));
    }

    @ExceptionHandler({IaFalhaException.class, IaIndisponivelException.class})
    public ResponseEntity<Map<String, Object>> iaIndisponivel(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("erro", "Serviço de IA temporariamente indisponível. Tente novamente mais tarde."));
    }
}
