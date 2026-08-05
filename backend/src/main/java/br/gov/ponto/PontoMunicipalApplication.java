package br.gov.ponto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Aplicacao principal do Ponto Municipal.
 *
 * <p>Monolito modular: cada subpacote de {@code br.gov.ponto} representa um modulo
 * de negocio (iam, tenant, cadastro, jornada, registro, apuracao, espelho,
 * relatorios, auditoria, integracao, billing, notificacao).</p>
 */
@SpringBootApplication
public class PontoMunicipalApplication {

    public static void main(String[] args) {
        SpringApplication.run(PontoMunicipalApplication.class, args);
    }
}
