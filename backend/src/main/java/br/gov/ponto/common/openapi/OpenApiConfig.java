package br.gov.ponto.common.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracao da documentacao OpenAPI/Swagger exposta em /swagger-ui.html.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pontoOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Ponto Municipal API")
                .description("SaaS de ponto eletronico (controle de frequencia) "
                        + "para servidores publicos municipais.")
                .version("0.1.0")
                .contact(new Contact().name("Ponto Municipal")));
    }
}
