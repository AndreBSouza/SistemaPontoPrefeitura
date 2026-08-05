package br.gov.ponto.common.web;

import br.gov.ponto.common.tenant.TenantContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Endpoint basico de informacao da aplicacao (fundacao). Util para validar o
 * boot, o roteamento e a resolucao de tenant antes da entrada dos modulos de negocio.
 */
@RestController
@RequestMapping("/api")
public class InfoController {

    @GetMapping("/info")
    public Map<String, Object> info() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("app", "Ponto Municipal");
        body.put("version", "0.1.0");
        String tenant = TenantContext.get();
        body.put("tenant", tenant == null ? "(nenhum)" : tenant);
        return body;
    }
}
