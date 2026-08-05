package br.gov.ponto.registro;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Gera o proximo NSR (Numero Sequencial de Registro) por tenant, de forma atomica.
 * Deve ser invocado dentro da transacao do registro (usa a conexao corrente).
 */
@Component
public class NsrGenerator {

    private final JdbcTemplate jdbcTemplate;

    public NsrGenerator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long next(UUID tenantId) {
        Long nsr = jdbcTemplate.queryForObject(
                "insert into nsr_sequencia (tenant_id, proximo_nsr) values (?, 1) "
                        + "on conflict (tenant_id) do update "
                        + "set proximo_nsr = nsr_sequencia.proximo_nsr + 1 "
                        + "returning proximo_nsr",
                Long.class, tenantId);
        if (nsr == null) {
            throw new IllegalStateException("Falha ao gerar NSR para o tenant " + tenantId);
        }
        return nsr;
    }
}
